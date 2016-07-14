package stamp.modelwiz;

import java.util.*;
import java.io.*;
import japa.parser.*;
import japa.parser.ast.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.body.*;
import japa.parser.ast.visitor.*;
import japa.parser.ast.type.*;


public class Main
{
	private File stubsDir;
	private File modelsDir;
	private Scanner scanIn;

	public static void main(String[] args) throws Exception
	{
		String stubsDirName = args[0];
		String modelsDirName = args[1];
		Main main = new Main(stubsDirName, modelsDirName);
		
		String topLevelClass = args[2];
		
		String methodName = null;
		if(args.length > 3){
			methodName = args[3];
			System.out.println("Method name: "+methodName);
		}

		String[] annot = null;
		if(args.length > 4){
			annot = args[4].split(",");
		}
		
		String code = null;
		if(args.length > 5){
			code = args[5];
		}

		main.perform(topLevelClass, methodName, annot, code);
	}

	Main(String stubsDirName, String modelsDirName)
	{
		this.stubsDir = new File(stubsDirName);
		this.modelsDir = new File(modelsDirName);
		this.scanIn = new Scanner(System.in);
	}

	void findAllTypes(TypeDeclaration type, Map<String,TypeDeclaration> allTypes, String outerClassName)
	{
		List<BodyDeclaration> members = type.getMembers();
		if(members == null)
			return;
		String name = outerClassName == null ? type.getName() : outerClassName+"."+type.getName();
		allTypes.put(name, type);
		
		for(BodyDeclaration mem : members){
			if(!(mem instanceof TypeDeclaration))
				continue;
			findAllTypes((TypeDeclaration) mem, allTypes, name);
		}
	}

	void perform(String topLevelClassName, String methodName, String[] annot, String code) throws Exception
	{
		File stubFile = getStubFile(topLevelClassName);
		if(!stubFile.exists()){
			System.out.println("Class does not exist!");
			scanIn.close();            
			return;
		}
		CompilationUnit stubCU = getCU(stubFile);

		Map<String,TypeDeclaration> stubTypes = new LinkedHashMap(); //preserves insertion order
		for(TypeDeclaration stubType : stubCU.getTypes()){
			findAllTypes(stubType, stubTypes, null);
		}

		if(stubTypes.isEmpty()){
			System.out.println("No classes were found!");
			return;
		}

		String[] allTypeNames = new String[stubTypes.size()];
		int count = 0;
		for(String name : stubTypes.keySet()){
			//System.out.println(count+". "+name);
			allTypeNames[count++] = name;
		}

		String chosenTypeName;
		if(count > 1){
			System.out.println("\n*****************");
			System.out.println("* Choose class *");
			System.out.println("****************");
			System.out.println("\nFollowing "+ count + " classes were found.");
			for(int i = 0; i < count; i++){
				System.out.println(i+". "+allTypeNames[i]);
			}
			System.out.print("\nEnter the index to choose the class: ");
			chosenTypeName = allTypeNames[getChoice(count-1)];
		} else {
			chosenTypeName = allTypeNames[0];
		}

		File modelFile = getModelFile(topLevelClassName);
		CompilationUnit modelCU;
		if(modelFile.exists())
			modelCU = getCU(modelFile);
		else{ 
			System.out.println("Model file not found.");
			modelCU = new CompilationUnit();
			modelCU.setPackage(stubCU.getPackage());
		}
		
		List<TypeDeclaration> modelTypes = modelCU.getTypes();
		if(modelTypes == null){
			modelTypes = new ArrayList();
			modelCU.setTypes(modelTypes);
		}
		TypeDeclaration stubType = stubTypes.get(chosenTypeName);
		TypeDeclaration modelType = findMatchingType(modelTypes, chosenTypeName);
		boolean flag = handleType(stubType, modelType, methodName, annot, code);
		if(flag)
			dump(modelFile, modelCU);
	}

	TypeDeclaration findMatchingType(List members, String typeName)
	{
		int i = typeName.indexOf('.');
		String prefix;
		if(i < 0){
			prefix = typeName;
			typeName = "";
		} else {
			prefix = typeName.substring(0, i);
			typeName = typeName.substring(i+1);
		}
		TypeDeclaration matchingType = null;		
		for(Iterator it = members.iterator(); it.hasNext();){
			BodyDeclaration decl = (BodyDeclaration) it.next();
			if(!(decl instanceof TypeDeclaration))
				continue;
			TypeDeclaration td = (TypeDeclaration) decl;
			if(prefix.equals(td.getName())){
				matchingType = td;
				break;
			}
		}
		if(matchingType == null){
			matchingType = new ClassOrInterfaceDeclaration();
			matchingType.setName(prefix);
			members.add(matchingType);
		}
		if(typeName.equals(""))
			return matchingType;
		else{
			members = matchingType.getMembers();
			if(members == null){
				members = new ArrayList();
				matchingType.setMembers(members);
			}
			return findMatchingType(members, typeName);
		}
	}

	boolean handleType(TypeDeclaration stubType, TypeDeclaration modelType, String methodName, String[] annot, String code) throws Exception
	{
		if(methodName == null) {
			System.out.println("\n*************************");
			System.out.println("* Choose method *");
			System.out.println("*************************");
			System.out.print("\nMethod name: ");
			methodName = scanIn.nextLine();
		}
		boolean approx = false;
		if(methodName.endsWith("*")){
			approx = true;
			methodName = methodName.substring(0, methodName.length()-1);
		}

		List<BodyDeclaration> matchedMethods = new ArrayList();
		List<BodyDeclaration> stubMembers = stubType.getMembers();
		if(stubMembers != null){
			for(BodyDeclaration stubMember : stubMembers){
				if(stubMember instanceof MethodDeclaration || 
				   stubMember instanceof ConstructorDeclaration){
					if(methodName.equals(signature(stubMember))){
						//signature match
						matchedMethods.add(stubMember);
						break;
					}
					String name = methodName(stubMember);
					boolean match = approx ? name.startsWith(methodName) : name.equals(methodName);
					if(match){
						matchedMethods.add(stubMember);
					}
				} 
			}
		}
		
		BodyDeclaration chosenMethod;
		if(matchedMethods.isEmpty()){
			System.out.println("No methods with matching name found.\n");
			return false;
		} 

		if(matchedMethods.size() == 1)
			chosenMethod = matchedMethods.get(0);
		else{
			int count = 0;
			System.out.println("\nFollowing "+ matchedMethods.size() + " methods with matching names found.");
			for(BodyDeclaration stubMember : matchedMethods){
				String stubMethSig = signature(stubMember); 
				System.out.println(count+++". "+stubMethSig);
			}
			System.out.print("\nEnter the index to choose the method: ");
			chosenMethod = matchedMethods.get(getChoice(count-1));
		}
		String sig = signature(chosenMethod);
		System.out.println("Chosen method: "+sig);
		
		//check if it already exist in model type
		List<BodyDeclaration> modelMembers = modelType.getMembers();
		if(modelMembers != null){
			for(BodyDeclaration modelMember : modelMembers){
				if(modelMember instanceof MethodDeclaration || 
				   modelMember instanceof ConstructorDeclaration){
					if(sig.equals(signature(modelMember))){
						System.out.println("Method already exist in the model class.");
						return false;
					}
				}
			}
		} else {
			modelMembers = new ArrayList();
			modelType.setMembers(modelMembers);
		}

		String newMethodBody = handleMethod(chosenMethod, annot, code);
		if(newMethodBody == null)
			return false;
		BodyDeclaration meth = parseMethod(newMethodBody);
		modelMembers.add(meth);
		return true;
	}

	String handleMethod(BodyDeclaration method, String[] annot, String code) throws Exception
	{
		System.out.println("\n********************************");
		System.out.println("* Choose annotations   *");
		System.out.println("********************************");
		
		StringBuilder newBody = new StringBuilder();
		List<String> annots = getAllAnnotations(method);
		if(annots.size() == 0){
			System.out.println("No possible annotations for this method");
		} else {
			int count = 0;
			System.out.println("Following are all possible annotations for the chosen method.");
			for(String an : annots){
				System.out.println(count+++". "+an);
			}
			int[] choices;
			if(annot != null){
				Set<String> chosenAnnots = new HashSet();
				for(String an : annot)
					chosenAnnots.add(an);

				choices = new int[annot.length];
				int j = 0;
				int i = 0;
				for(String an : annots){
					if(chosenAnnots.contains(an))
						choices[j++] = i;
					i++;
				}
			} else {
				System.out.print("\nEnter the white-space-separated indices of annotations (Press enter to not accept any): ");
				choices = getChoices(count-1);
			}
			if(choices == null){
				System.out.println("No annotations to be added.");
			} else {
				System.out.println("Following annotation to be added:");
				newBody.append("@STAMP(flows={");
				boolean first = true;
				for(int c : choices){
					System.out.println(c+". "+annots.get(c));
					String[] toks = annots.get(c).split(" => ");
					String an = "@Flow(from=\""+toks[0]+"\", to=\""+toks[1]+"\")";
					if(!first)
						newBody.append(", ");
					newBody.append(an);
					first = false;
				}
				newBody.append("})\n");
			}
		}

		if(code == null){
			System.out.println("\n*********************************************************");
			System.out.println("* Enter code. Type ^d at the end of your input. *");
			System.out.println("*********************************************************");
			
			StringBuilder codeBuilder = new StringBuilder();
			boolean first = true;
			System.out.print("\t");
			while (scanIn.hasNext()) {
				if(!first)
					codeBuilder.append("\n");
				System.out.print("\t");
				codeBuilder.append(scanIn.nextLine());
				first = false;
			}
			code = codeBuilder.toString();
		}

		newBody.append(method.toString().replace("throw new RuntimeException(\"Stub!\");", 
												 code));
		
		System.out.println("\n*******************");
		System.out.println("* Confirm *");
		System.out.println("*******************");
		
		System.out.println("Following code will be added to the model class.\n");
		System.out.println(newBody);
		System.out.print("\nEnter your choice (y/n): ");
		this.scanIn = new Scanner(System.in);
		String input = scanIn.nextLine();
		if(!input.equals("y")){
			System.out.println("No model was added.");
			return null;
		} else {
			return newBody.toString();
		}	
	}
	
	int[] getChoices(int maxChoice)
	{
		outer:
		do{
			try{
				String input = scanIn.nextLine();
				if(input.trim().length() == 0)
					return null;
				String[] tokens = input.split(" ");
				int[] choices = new int[tokens.length];
				int i = 0;
				for(String token : tokens){
					int choice = Integer.parseInt(token);
					if(choice >= 0 && choice <= maxChoice)
						choices[i++] = choice;
					else{
						System.out.println("Expecting a list of integers in the range of [0,"+maxChoice+"], separated by blank spaces.");
						continue outer;
					}
				}
				return choices;
			}catch(NumberFormatException e){
				System.out.println("Expecting a list of integers in the range of [0,"+maxChoice+"], separated by blank spaces.");
			}
		}while(true);
	}

	int getChoice(int maxChoice)
	{
		do{
			try{
				String input = scanIn.nextLine();
				String[] tokens = input.split(" ");
				if(tokens.length > 1){
					System.out.println("Expecting an integer input in the range [0,"+maxChoice+"].");
					continue;
				}
				int choice = Integer.parseInt(tokens[0]);
				if(choice >= 0 && choice <= maxChoice)
					return choice;
				System.out.println("Expecting an integer input in the range [0,"+maxChoice+"].");
			}catch(NumberFormatException e){
				System.out.println("Expecting an integer input in the range [0,"+maxChoice+"].");
			}
		}while(true);
	}

	File getStubFile(String className)
	{
		String filePath = className.replace('.', '/')+".java";
		File stubFile = new File(stubsDir, filePath);
		return stubFile;
	}

	File getModelFile(String className)
	{
		String filePath = className.replace('.', '/')+".java";
		File stubFile = new File(modelsDir, filePath);
		System.out.println(stubFile);
		return stubFile;
	}

	CompilationUnit getCU(File file) throws Exception
	{
        FileInputStream in = new FileInputStream(file);

        CompilationUnit cu;
        try {
            cu = JavaParser.parse(in);
        } finally {
            in.close();
        }
		return cu;
	}

	private String methodName(BodyDeclaration meth)
	{
		if(meth instanceof MethodDeclaration){
			MethodDeclaration method = (MethodDeclaration) meth;
			return method.getName();
		}
		else if(meth instanceof ConstructorDeclaration){
			ConstructorDeclaration method = (ConstructorDeclaration) meth;
			return "<init>";
		}
		return null;
	}

	private List<String> getAllAnnotations(BodyDeclaration meth)
	{
		List<Parameter> params;
		boolean isVoidReturnType = true;
		boolean isStatic = false;
		if(meth instanceof MethodDeclaration){
			MethodDeclaration method = (MethodDeclaration) meth;
			params = method.getParameters();
			isStatic = ModifierSet.isStatic(method.getModifiers());
			isVoidReturnType = method.getType().toString().equals("void");
		}
		else if(meth instanceof ConstructorDeclaration){
			ConstructorDeclaration method = (ConstructorDeclaration) meth;
			params = method.getParameters();
		}		
		else
			throw new RuntimeException("");
		return getAllAnnotations(params, isStatic, isVoidReturnType);
	}

	private List<String> getAllAnnotations(List<Parameter> params, boolean isStatic, boolean isVoidReturnType)
	{
		//System.out.println("isStatic " +isStatic);
		List<String> annots = new ArrayList();
		int paramCount = params == null ? 0 : params.size();
		for(int i = 0; i < paramCount; i++){
			for(int j = 0; j < paramCount; j++){
				if(i == j)
					continue;
				annots.add(params.get(i).getId().getName() + " => " + params.get(j).getId().getName());
			}
			if(!isStatic)
				annots.add(params.get(i).getId().getName() + " => this");
			if(!isVoidReturnType)
				annots.add(params.get(i).getId().getName() + " => @return");
		}
		if(!isStatic){
			for(int i = 0; i < paramCount; i++){
				annots.add("this => "+params.get(i).getId().getName());
			}
			if(!isVoidReturnType)
				annots.add("this => return");
		}
		return annots;
	}

	private String signature(BodyDeclaration meth)
	{
		if(meth instanceof MethodDeclaration){
			MethodDeclaration method = (MethodDeclaration) meth;
			return signature(method.getName(), method.getType(), method.getParameters());
		}
		else if(meth instanceof ConstructorDeclaration){
			ConstructorDeclaration method = (ConstructorDeclaration) meth;
			return signature("<init>", new VoidType(), method.getParameters());
		}
		else if(meth instanceof InitializerDeclaration){
			return "<clinit>";
		}
		throw new RuntimeException(meth.toString());
	}

	private String signature(String name, Type retType, List<Parameter> params)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(retType.toString()+" ");
		builder.append(name);
		builder.append("(");
		
		if(params != null){
			int i = params.size();
			for(Parameter p : params){
				builder.append(p.getType().toString()+" "+p.getId().getName());
				if(i > 1)
					builder.append(", ");
				i--;
			}
		}
		builder.append(")");
		return builder.toString();
	}

	public static BodyDeclaration parseMethod(String newMethodBody) throws ParseException 
	{ 
		String dummyClassDecl = "class Dummy { " + newMethodBody + " }";
		InputStream stream = new ByteArrayInputStream(dummyClassDecl.getBytes());
		CompilationUnit cu = JavaParser.parse(stream);
		return (BodyDeclaration) cu.getTypes().get(0).getMembers().get(0);
	} 

    private void dump(File outFile, CompilationUnit cu) throws Exception
    {
        File parent = outFile.getParentFile();
		parent.mkdirs();
        PrintWriter writer = new PrintWriter(new FileWriter(outFile));
        writer.println(cu.toString());
        writer.close();
    }
}