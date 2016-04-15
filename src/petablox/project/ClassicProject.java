package petablox.project;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import petablox.bddbddb.Dom;
import petablox.bddbddb.RelSign;
import petablox.logicblox.LogicBloxUtils;
import petablox.project.ITask;
import petablox.project.analyses.DlogAnalysis;
import petablox.project.analyses.ProgramDom;
import petablox.project.analyses.ProgramRel;
import petablox.util.ArraySet;
import petablox.util.Timer;
import petablox.util.Utils;

/**
 * A Petablox project comprising a set of tasks and a set of targets
 * produced/consumed by those tasks.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ClassicProject extends Project {
    private static final String CANNOT_INSTANTIATE_TASK =
        "ERROR: ClassicProject: Cannot instantiate task '%s': %s.";
    private static final String CANNOT_INSTANTIATE_TRGT =
        "ERROR: ClassicProject: Cannot instantiate trgt '%s': %s.";
    private static final String MULTIPLE_TASKS_PRODUCING_TRGT =
        "ERROR: ClassicProject: Multiple tasks (%s) producing target '%s'; include exactly one of them via property 'petablox.run.analyses'.";
    private static final String TASK_PRODUCING_TRGT_NOT_FOUND =
        "ERROR: ClassicProject: No task producing target '%s' found in project.";
    private static final String TASK_NOT_FOUND =
        "ERROR: ClassicProject: Task named '%s' not found in project.";
    private static final String TRGT_NOT_FOUND =
        "ERROR: ClassicProject: Target named '%s' not found in project.";

    private ClassicProject() { }

    private static ClassicProject project = null;

    public static ClassicProject g() {
        if (project == null)
            project = new ClassicProject();
        return project;
    }

    private final Map<String, ITask> nameToTaskMap = new HashMap<String, ITask>();
    private final Map<String, Object> nameToTrgtMap = new HashMap<String, Object>();
    private final Map<ITask, List<Object>> taskToProducedTrgtsMap = new HashMap<ITask, List<Object>>();
    private final Map<ITask, List<Object>> taskToConsumedTrgtsMap = new HashMap<ITask, List<Object>>();
    private final Map<Object, Set<ITask>> trgtToProducingTasksMap = new HashMap<Object, Set<ITask>>();
    private final Map<Object, Set<ITask>> trgtToConsumingTasksMap = new HashMap<Object, Set<ITask>>();
    private final Set<ITask> doneTasks = new HashSet<ITask>();
    private final Set<Object> doneTrgts = new HashSet<Object>();
    private boolean isBuilt = false;
    private String lastTaskName;
    private ITask  lastTask;

    @Override
    public void build() {
        if (isBuilt)
            return;
        
        LogicBloxUtils.validateMultiPgmOptions();
        switch (Config.datalogEngine) {
        case LOGICBLOX3:
        case LOGICBLOX4:
            LogicBloxUtils.initializeWorkspace(Config.logicbloxWorkspace);
            LogicBloxUtils.setMultiTag();           
            break;
        case BDDBDDB:
            break;
        }
        
        TaskParser taskParser = new TaskParser();
        if (!taskParser.run())
            abort();

        // build nameToTaskMap
        Map<String, Class<ITask>> nameToJavaTaskMap = taskParser.getNameToJavaTaskMap();
        Map<String, DlogAnalysis> nameToDlogTaskMap = taskParser.getNameToDlogTaskMap();
        if (!buildNameToTaskMap(nameToJavaTaskMap, nameToDlogTaskMap))
            abort();

        Map<String, Set<TrgtInfo>> nameToTrgtInfosMap = taskParser.getNameToTrgtInfosMap();
        TrgtParser trgtParser = new TrgtParser(nameToTrgtInfosMap);
        if (!trgtParser.run())
            abort();

        // build nameToTrgtMap
        Map<String, Class> nameToTrgtTypeMap = trgtParser.getNameToTrgtTypeMap();
        if (!buildNameToTrgtMap(nameToTrgtTypeMap))
            abort();

        // set signs and doms of program relation targets
        Map<String, RelSign> nameToTrgtSignMap = trgtParser.getNameToTrgtSignMap();
        setRelSignsAndDoms(nameToTrgtSignMap);

        // build auxiliary maps between targets and tasks
        Map<String, List<String>> nameToConsumeNamesMap = taskParser.getNameToConsumeNamesMap();
        Map<String, List<String>> nameToProduceNamesMap = taskParser.getNameToProduceNamesMap();
        buildDerivedMaps(nameToConsumeNamesMap, nameToProduceNamesMap);

        isBuilt = true;
    }

    @Override
    public void run(String[] taskNames) {
    	int sz = taskNames.length;
    	lastTaskName = taskNames[sz - 1];
        for (String name : taskNames)
            runTask(name);
    }

    @Override
    public void print() {
        build();
        PrintWriter out;
        out = OutDirUtils.newPrintWriter("targets.xml");
        out.println("<targets " +
            "java_analysis_path=\"" + Config.javaAnalysisPathName + "\" " +
            "dlog_analysis_path=\"" + Config.dlogAnalysisPathName + "\">");
        for (String name : nameToTrgtMap.keySet()) {
            Object trgt = nameToTrgtMap.get(name);
            String kind;
            if (trgt instanceof ProgramDom)
                kind = "domain";
            else if (trgt instanceof ProgramRel)
                kind = "relation";
            else
                kind = "other";
            Set<ITask> tasks = trgtToProducingTasksMap.get(trgt);
            Iterator<ITask> it = tasks.iterator();
            String producerStr;
            String otherProducersStr = "";
            if (it.hasNext()) {
                ITask fstTask = it.next();
                producerStr = getNameAndURL(fstTask);
                while (it.hasNext()) {
                    ITask task = it.next();
                    otherProducersStr += "<producer " + getNameAndURL(task) + "/>";
                }
            } else
                producerStr = "producer_name=\"-\" producer_url=\"-\"";
            out.println("\t<target name=\"" + name + "\" kind=\"" + kind +
                "\" " + producerStr  + ">" +
                otherProducersStr + "</target>");
        }
        out.println("</targets>");
        out.close();
        out = OutDirUtils.newPrintWriter("taskgraph.dot");
        out.println("digraph G {");
        for (String name : nameToTrgtMap.keySet()) {
            String trgtId = "\"" + name + "_trgt\"";
            out.println(trgtId + "[label=\"\",shape=ellipse,style=filled,color=blue];");
            Object trgt = nameToTrgtMap.get(name);
            for (ITask task : trgtToProducingTasksMap.get(trgt)) {
                String taskId = "\"" + task.getName() + "_task\"";
                out.println(taskId + " -> " + trgtId + ";");
            }
            for (ITask task : trgtToConsumingTasksMap.get(trgt)) {
                String taskId = "\"" + task.getName() + "_task\"";
                out.println(trgtId + " -> " + taskId + ";");
            }
        }
        for (String name : nameToTaskMap.keySet()) {
            String taskId = "\"" + name + "_task\"";
            out.println(taskId + "[label=\"\",shape=square,style=filled,color=red];");
        }
        out.println("}");
        out.close();
        OutDirUtils.copyResourceByName("web/style.css");
        OutDirUtils.copyResourceByName("web/targets.xsl");
        OutDirUtils.copyResourceByName("web/targets.dtd");
        OutDirUtils.runSaxon("targets.xml", "targets.xsl");
    }

    private boolean buildNameToTaskMap(
            Map<String, Class<ITask>> nameToJavaTaskMap,
            Map<String, DlogAnalysis> nameToDlogTaskMap) {
        boolean hasNoErrors = true;
        for (Map.Entry<String, Class<ITask>> entry :
                nameToJavaTaskMap.entrySet()) {
            String name = entry.getKey();
            Class<ITask> type = entry.getValue();
            ITask task = null;
            Exception ex = null;
            try {
                task = type.newInstance();
            } catch (InstantiationException e) {
                ex = e;
            } catch (IllegalAccessException e) {
                ex = e;
            }
            if (ex != null) {
                Messages.log(CANNOT_INSTANTIATE_TASK, name, ex.getMessage());
                hasNoErrors = false;
            } else {
                assert (task != null);
                nameToTaskMap.put(name, task);
                task.setName(name);
                
            }
        }
        for (Map.Entry<String, DlogAnalysis> entry :
                nameToDlogTaskMap.entrySet()) {
            String name = entry.getKey();
            DlogAnalysis task = entry.getValue();
            nameToTaskMap.put(name, task);
        }
        return hasNoErrors;
    }

    private boolean buildNameToTrgtMap(Map<String, Class> nameToTrgtTypeMap) {
        boolean hasNoErrors = true;
        for (Map.Entry<String, Class> entry : nameToTrgtTypeMap.entrySet()) {
            String name = entry.getKey();
            Object trgt = nameToTaskMap.get(name);
            if (trgt != null) {
                nameToTrgtMap.put(name, trgt);
                continue;
            } 
            Class type = entry.getValue();
            Exception ex = null;
            try {
                trgt = type.newInstance();
            } catch (InstantiationException e) {
                ex = e;
            } catch (IllegalAccessException e) {
                ex = e;
            }
            if (ex != null) {
                Messages.log(CANNOT_INSTANTIATE_TRGT, name, ex.getMessage());
                hasNoErrors = false;
            } else {
                assert (trgt != null);
                nameToTrgtMap.put(name, trgt);
                if (trgt instanceof ITask) {
                    ITask task = (ITask) trgt;
                    task.setName(name);
                }
            }
        }
        return hasNoErrors;
    }
    
    private void setRelSignsAndDoms(Map<String, RelSign> nameToTrgtSignMap) {
        for (Map.Entry<String, RelSign> entry : nameToTrgtSignMap.entrySet()) {
            String name = entry.getKey();
            RelSign sign = entry.getValue();
            ProgramRel rel = (ProgramRel) nameToTrgtMap.get(name);
            String msg = "rel is null in nameToTrgtMap for " + name;
            assert (rel != null) : msg;
            String[] domNames = sign.getDomNames();
            int n = domNames.length;
            ProgramDom[] doms = new ProgramDom[n];
            for (int i = 0; i < n; i++) {
                String domName = Utils.trimNumSuffix(domNames[i]);
                ProgramDom dom = (ProgramDom) nameToTrgtMap.get(domName);
                assert (dom != null);
                doms[i] = (ProgramDom) dom;
            }
            rel.setSign(sign);
            rel.setDoms(doms);
        }
    }

    // builds the following maps:
    //     trgtToConsumerTasksMap, trgtToProducerTasksMap
    //     taskToConsumedTrgtsMap, taskToProduceducedTrgtsMap
    // uses the following maps:
    //     nameToTrgtMap, nameToTaskMap
    //    taskNameToProduceNamesMap, taskNameToConsumeNamesMap
    private void buildDerivedMaps(
            Map<String, List<String>> taskNameToConsumeNamesMap,
            Map<String, List<String>> taskNameToProduceNamesMap) {
        for (Object trgt : nameToTrgtMap.values()) {
            Set<ITask> consumerTasks = new HashSet<ITask>();
            trgtToConsumingTasksMap.put(trgt, consumerTasks);
            Set<ITask> producerTasks = new HashSet<ITask>();
            trgtToProducingTasksMap.put(trgt, producerTasks);
        }
        for (String taskName : nameToTaskMap.keySet()) {
        	ITask task = nameToTaskMap.get(taskName);
            List<String> consumedNames = taskNameToConsumeNamesMap.get(taskName);
            List<Object> consumedTrgts = new ArrayList<Object>(consumedNames.size());
            for (String name : consumedNames) {
                Object trgt = nameToTrgtMap.get(name);
                String msg = "Target null for " + name + " consumed by task " + taskName;
                assert (trgt != null) : msg;
                consumedTrgts.add(trgt);
                Set<ITask> consumerTasks = trgtToConsumingTasksMap.get(trgt);
                consumerTasks.add(task);
            }
            taskToConsumedTrgtsMap.put(task, consumedTrgts);
            List<String> producedNames = taskNameToProduceNamesMap.get(taskName);
            List <Object> producedTrgts =
                new ArrayList<Object>(producedNames.size());
            for (String name : producedNames) {
                Object trgt = nameToTrgtMap.get(name);
                String msg = "Target null for " + name + " produced by task " + taskName;
                assert (trgt != null) : msg;
                producedTrgts.add(trgt);
                Set<ITask> producerTasks = trgtToProducingTasksMap.get(trgt);
                producerTasks.add(task);
            }
            taskToProducedTrgtsMap.put(task, producedTrgts);
        }
        for (String trgtName : nameToTrgtMap.keySet()) {
            Object trgt = nameToTrgtMap.get(trgtName);
            Set<ITask> producerTasks = trgtToProducingTasksMap.get(trgt);
            int pSize = producerTasks.size();
            if (pSize == 0) {
                Set<ITask> consumerTasks = trgtToConsumingTasksMap.get(trgt);
                int cSize = consumerTasks.size();
                List<String> consumerTaskNames = new ArraySet<String>(cSize);
                for (ITask task : consumerTasks)
                    consumerTaskNames.add(getSourceName(task));
                undefinedTarget(trgtName, consumerTaskNames);
            } else if (pSize > 1) {
                List<String> producerTaskNames = new ArraySet<String>(pSize);
                for (ITask task : producerTasks) {
                    producerTaskNames.add(getSourceName(task));
                }
                redefinedTarget(trgtName, producerTaskNames);
            }
        }
    }

    @Override
    public void printRels(String[] relNames) {
        build();
        for (String relName : relNames) {
            ProgramRel rel = (ProgramRel) nameToTrgtMap.get(relName);
            if (rel == null)
                Messages.fatal("Failed to load relation " + relName);
            rel.load();
            rel.print();
        }
    }

    public Object getTrgt(String name) {
        build();
        Object trgt = nameToTrgtMap.get(name);
        if (trgt == null && Config.populate) {
        	trgt = nameToTrgtMap.get(Config.multiTag + name);
        }
        if (trgt == null)
        	Messages.fatal(TRGT_NOT_FOUND, name);
        return trgt;
    }

    public ITask getTask(String name) {
        build();
        ITask task = nameToTaskMap.get(name);
        if (task == null) 
            Messages.fatal(TASK_NOT_FOUND, name);
        return task;
    }

    public ITask getTaskProducingTrgt(Object trgt) {
        Set<ITask> tasks = trgtToProducingTasksMap.get(trgt);
        int n = tasks.size();
        if (n > 1) {
            String tasksStr = "";
            for (ITask task : tasks)
                tasksStr += " " + task.getName();
            Messages.fatal(MULTIPLE_TASKS_PRODUCING_TRGT, tasksStr.substring(1), trgt.toString());
        }
        if (n == 0)
            Messages.fatal(TASK_PRODUCING_TRGT_NOT_FOUND, trgt.toString());
        return tasks.iterator().next();
    }

    public void runTask(ITask task) {
        if (isTaskDone(task)) {
            if (Config.verbose >= 1)
                System.out.println("TASK " + task + " ALREADY DONE.");
            return;
        }
        Timer timer = new Timer(task.getName());
        if (Config.verbose >= 1)
            System.out.println("ENTER: " + task + " at " + (new Date()));
        timer.init();
        timer.pause();
        List<Object> consumedTrgts = taskToConsumedTrgtsMap.get(task);
        for (Object trgt : consumedTrgts) {
            if (isTrgtDone(trgt))
                continue;
            if (Config.reuseRels && trgt instanceof ProgramRel) {
                ProgramRel rel = (ProgramRel) trgt;
                File file = new File(Config.bddbddbWorkDirName, rel.getName() + ".bdd");
                if (file.exists()) {
                    for (Dom dom : rel.getDoms()) {
                        ITask task2 = getTaskProducingTrgt(dom);
                        runTask(task2);
                    }
                    setTrgtDone(trgt);
                    continue;
                }
            }
            ITask task2 = getTaskProducingTrgt(trgt);
            runTask(task2);
        }
        timer.resume();
        if (!(Config.populate && task == lastTask && Config.crossPgmAnalysis)) {
        	if (!Config.analyze)
        		task.run();
        	else if (Config.analyze && task == lastTask)
        		task.run();
        } else
        	System.out.println("Skipping task: " + task.getName() + " because it is the \"populate\" phase of Multiple Program Support.");
        timer.done();
        if (Config.verbose >= 1) {
            System.out.println("LEAVE: " + task);
            printTimer(timer);
        }
        setTaskDone(task);
        List<Object> producedTrgts = taskToProducedTrgtsMap.get(task);
        assert(producedTrgts != null);
        for (Object trgt : producedTrgts) {
            setTrgtDone(trgt);
        }
    }

    private static void printTimer(Timer timer) {
        System.out.println("Exclusive time: " + timer.getExclusiveTimeStr());
        System.out.println("Inclusive time: " + timer.getInclusiveTimeStr());
    }
    
    public ITask runTask(String name) {
        ITask task = getTask(name);
        if (name.equals(lastTaskName)) lastTask = task;
        runTask(task);
        return task;
    }

    public boolean isTrgtDone(Object trgt) {
        return doneTrgts.contains(trgt);
    }

    public boolean isTrgtDone(String name) {
        return isTrgtDone(getTrgt(name));
    }

    public void setTrgtDone(Object trgt) {
        doneTrgts.add(trgt);
    }

    public void setTrgtDone(String name) {
        setTrgtDone(getTrgt(name));
    }

    public void resetTrgtDone(Object trgt) {
        if (doneTrgts.remove(trgt)) {
            for (ITask task : trgtToConsumingTasksMap.get(trgt)) {
                resetTaskDone(task);
            }
        }
    }

    public void resetAll() {
        doneTrgts.clear();
        doneTasks.clear();
    }

    public void resetTrgtDone(String name) {
        resetTrgtDone(getTrgt(name));
    }

    public boolean isTaskDone(ITask task) {
        return doneTasks.contains(task);
    }

    public boolean isTaskDone(String name) {
        return isTaskDone(getTask(name));
    }

    public void setTaskDone(ITask task) {
        doneTasks.add(task);
    }

    public void setTaskDone(String name) {
        setTaskDone(getTask(name));
    }

    public void resetTaskDone(ITask task) {
        if (doneTasks.remove(task)) {
            for (Object trgt : taskToProducedTrgtsMap.get(task)) {
                resetTrgtDone(trgt);
            }
        }
    }

    public Set<String> getTargetNames() {
        return Collections.unmodifiableSet(nameToTrgtMap.keySet());
    }
    
    public Set<String> getTaskNames() {
        return Collections.unmodifiableSet(nameToTaskMap.keySet());
    }
    
    public Set<String> getFinishedTargetNames() {
        HashSet<String> names = new HashSet<String>(Math.max((int) (doneTrgts.size()/.75f) + 1, 16), 0.75f);
        for (String name: nameToTrgtMap.keySet())
            if (isTrgtDone(name))
                names.add(name);
        return names;
    }
    
    public Set<String> getFinishedTaskNames() {
        HashSet<String> names = new HashSet<String>(Math.max((int) (doneTasks.size()/.75f) + 1, 16), 0.75f);
        for (String name: nameToTaskMap.keySet())
            if (isTrgtDone(name))
                names.add(name);
        return names;
    }
    
    public void resetTaskDone(String name) {
        resetTaskDone(getTask(name));
    }

    private static final FilenameFilter filter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            if (name.startsWith("."))
                return false;
            return true;
        }
    };
    
    private static String getNameAndURL(ITask task) {
        Class clazz = task.getClass();
        String loc;
        if (clazz == DlogAnalysis.class) {
            loc = ((DlogAnalysis) task).getFileName();
            loc = (new File(loc)).getName();
        } else
            loc = clazz.getName().replace(".", "/") + ".html";
        loc = Config.javadocURL + loc;
        return "producer_name=\"" + task.getName() +
            "\" producer_url=\"" + loc + "\"";
    }

    private static String getSourceName(ITask analysis) {
        Class clazz = analysis.getClass();
        if (clazz == DlogAnalysis.class)
            return ((DlogAnalysis) analysis).getFileName();
        return clazz.getName();
    }

    private void undefinedTarget(String name, List<String> consumerTaskNames) {
        if (Config.verbose >= 2) {
            String msg = "WARNING: '" + name + "' not declared as produced name of any task";
            if (consumerTaskNames.isEmpty())
                msg += "\n";
            else {
                msg += "; declared as consumed name of following tasks:\n";
                for (String taskName : consumerTaskNames)
                    msg += "\t'" + taskName + "'\n";
            }
            Messages.log(msg);
        }
    }
    
    private void redefinedTarget(String name, List<String> producerTaskNames) {
        if (Config.verbose >= 2) {
            String msg = "WARNING: '" + name + "' declared as produced name of multiple tasks:\n";
            for (String taskName : producerTaskNames) 
                msg += "\t'" + taskName + "'\n";
            Messages.log(msg);
        }
    }
}
