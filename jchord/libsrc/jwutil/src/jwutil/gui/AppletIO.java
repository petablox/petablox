// AppletIO.java, created Oct 5, 2004 8:40:10 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.gui;

import java.awt.Dimension;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import jwutil.io.FillableReader;
import jwutil.io.ReaderInputStream;
import jwutil.reflect.Reflect;

/**
 * AppletIO
 * 
 * @author jwhaley
 * @version $Id: AppletIO.java,v 1.7 2005/04/29 02:32:27 joewhaley Exp $
 */
public class AppletIO extends JApplet {
    
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 4121128139021169972L;
    
    public static String DEFAULT_ENCODING = "UTF-8";
    
    /**
     * AppletWriter takes anything written to it and puts it in the output area.
     * 
     * @author jwhaley
     * @version $Id: AppletIO.java,v 1.7 2005/04/29 02:32:27 joewhaley Exp $
     */
    public class AppletWriter extends Writer {
        /* (non-Javadoc)
         * @see java.io.Writer#write(char[], int, int)
         */
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (len == 0) {
                return;
            }
            String str = new String(cbuf, off, len);
            outputArea.append(str);
            jumpToEndOfOutput();
        }

        /* (non-Javadoc)
         * @see java.io.Flushable#flush()
         */
        public void flush() throws IOException {
            // Nothing to do.
        }

        /* (non-Javadoc)
         * @see java.io.Closeable#close()
         */
        public void close() throws IOException {
            // Nothing to do.
        }
    }
    
    /**
     * AppletOutputStream takes anything written to it and puts it in the output
     * area.
     * 
     * TODO: The write() method doesn't handle multibyte characters correctly.
     * If you want correct usage of multibyte characters, use AppletWriter
     * instead.
     * 
     * @author jwhaley
     * @version $Id: AppletIO.java,v 1.7 2005/04/29 02:32:27 joewhaley Exp $
     */
    public class AppletOutputStream extends OutputStream {
        
        /* (non-Javadoc)
         * @see java.io.OutputStream#write(int)
         */
        public void write(int b) throws IOException {
            outputArea.append(new String(new byte[]{(byte) b}));
            jumpToEndOfOutput();
        }

        /* (non-Javadoc)
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        public void write(byte b[], int off, int len) throws IOException {
            if (len == 0) {
                return;
            }
            String str = new String(b, off, len, DEFAULT_ENCODING);
            outputArea.append(str);
            jumpToEndOfOutput();
        }
    }
    
    /**
     * Listens for newline inputs in the input area, and sends that line
     * to inputWriter.
     * 
     * @author jwhaley
     * @version $Id: AppletIO.java,v 1.7 2005/04/29 02:32:27 joewhaley Exp $
     */
    public class TextAreaListener implements DocumentListener {
    
        /* (non-Javadoc)
         * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
         */
        public void insertUpdate(DocumentEvent e) {
            int length = e.getLength();
            if (length == 0) return;
            int offset = e.getOffset();
            try {
                String s = inputArea.getText(offset, length);
                int i;
                while ((i = s.indexOf('\n')) >= 0) {
                    int lineNum = inputArea.getLineOfOffset(offset);
                    int sOff = inputArea.getLineStartOffset(lineNum);
                    int eOff = inputArea.getLineEndOffset(lineNum);
                    String line = inputArea.getText(sOff, eOff - sOff);
                    if (outputArea != null) {
                        outputArea.append(line);
                        jumpToEndOfOutput();
                    }
                    if (inputWriter != null) {
                        inputWriter.write(line);
                    }
                    offset = eOff;
                    s = s.substring(i + 1);
                }
            } catch (IOException x) {
                x.printStackTrace();
            } catch (BadLocationException x) {
                x.printStackTrace();
            }
        }

        /* (non-Javadoc)
         * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
         */
        public void removeUpdate(DocumentEvent e) {
            // Ignore.
        }

        /* (non-Javadoc)
         * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
         */
        public void changedUpdate(DocumentEvent e) {
            // Ignore.
        }
    }
 
    /**
     * Scroll to the end of the output area.
     */
    public void jumpToEndOfOutput() {
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
    
    JTextArea outputArea;
    JTextArea inputArea;
    Writer inputWriter;
    Method method;
    Object[] methodArgs;

    protected void loadAppletParameters() {
        // Get the applet parameters.
        String className = getParameter("class");
        String methodName = getParameter("method");
        if (methodName == null) methodName = "main";
        method = Reflect.getDeclaredMethod(className, methodName);
        Class[] pTypes = method.getParameterTypes();
        methodArgs = new Object[pTypes.length];
        Object[] args;
        if (pTypes.length == 1 && pTypes[0] == String[].class) {
            int nParams = 0;
            while (getParameter("arg"+nParams) != null)
                ++nParams;
            methodArgs[0] = args = new String[nParams];
        } else {
            args = methodArgs;
        }
        for (int i = 0; i < args.length; ++i) {
            String arg = getParameter("arg"+i);
            args[i] = arg;
        }
    }
    
    public void init() {
        loadAppletParameters();
        
        //Execute a job on the event-dispatching thread:
        //creating this applet's GUI.
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    PrintStream out = createGUI();
                    launch(out, method, methodArgs);
                }
            });
        } catch (Exception e) { 
            System.err.println("createGUI didn't successfully complete");
        }
    }
    
    PrintStream createGUI() {
        outputArea = new JTextArea();
        outputArea.setMargin(new Insets(5, 5, 5, 5));
        outputArea.setEditable(false);
        inputArea = new JTextArea();
        inputArea.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane top = new JScrollPane(outputArea);
        JScrollPane bottom = new JScrollPane(inputArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        //splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(1.0);
        getContentPane().add(splitPane);
        // Provide minimum sizes for the two components in the split pane.
        Dimension minimumSize = new Dimension(400, 40);
        top.setMinimumSize(minimumSize);
        bottom.setMinimumSize(minimumSize);
        bottom.setPreferredSize(minimumSize);
        // Use this listener to listen to changes to the text area.
        DocumentListener myListener = new TextAreaListener();
        inputArea.getDocument().addDocumentListener(myListener);
        // Redirect System.out/System.err to our text area.
        try {
            InputStream in;
            FillableReader fin = new FillableReader();
            inputWriter = fin.getWriter();
            in = new ReaderInputStream(fin);

            // Support for JDK1.3, which doesn't allow encoding on PrintStream
            PrintStream out, err;
            try {
                err = out = new PrintStream(new AppletOutputStream(), true, DEFAULT_ENCODING);
            } catch (NoSuchMethodError _) {
                err = out = new PrintStream(new AppletOutputStream(), true);
            }
            try {
                System.setOut(out);
                System.setErr(err);
                // Redirect System.in from our input area.
                System.setIn(in);
            } catch (SecurityException x) {
                //outputArea.append("Note: Cannot reset stdio: " + x.toString());
                x.printStackTrace();
                initStreams(method.getDeclaringClass(), in, out, err);
            }
            return out;
        } catch (UnsupportedEncodingException x) {
            outputArea.append(x.toString());
            x.printStackTrace();
            return null;
        }
    }
    
    public static void main(String[] s) throws SecurityException,
        NoSuchMethodException, ClassNotFoundException {
        AppletIO applet = new AppletIO();
        
        if (s.length < 1) {
            applet.method = AppletIO.class.getDeclaredMethod("example", new Class[0]);
            applet.methodArgs = new Object[0];
        } else {
            applet.method = Class.forName(s[0]).getDeclaredMethod("main",
                new Class[]{String[].class});
            String[] s2 = new String[s.length - 1];
            System.arraycopy(s, 1, s2, 0, s2.length);
            applet.methodArgs = new Object[]{s2};
        }
        
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(applet);
        f.setSize(500, 400);
        f.setLocation(200, 200);
        applet.createGUI();
        f.setVisible(true);
        
        System.out.println("Starting " + applet.method.getDeclaringClass().getName()+
            "."+applet.method.getName()+"()");
        launch(System.out, applet.method, applet.methodArgs);
    }
    
    public static void initStreams(Class c, InputStream in, PrintStream out, PrintStream err) {
        Field f;
        try {
            f = c.getDeclaredField("in");
            f.set(null, in);
        }
        catch (NoSuchFieldException _) {}
        catch (IllegalAccessException _) {}
        try {
            f = c.getDeclaredField("out");
            f.set(null, out);
        }
        catch (NoSuchFieldException _) {}
        catch (IllegalAccessException _) {}
        try {
            f = c.getDeclaredField("err");
            f.set(null, err);
        }
        catch (NoSuchFieldException _) {}
        catch (IllegalAccessException _) {}
    }
    
    public static void launch(final PrintStream out, final Method m, final Object[] args) {
        new Thread() {
            public void run() {
                try {
                    m.invoke(null, args);
                } catch (InvocationTargetException e) {
                    e.getTargetException().printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } finally {
                    if (out != null) out.println("Terminated.");
                }
            }
        }.start();
    }
    
    // For an example: A method that just consumes System.in.
    public static void example() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                System.in, DEFAULT_ENCODING));
            for (;;) {
                String s = in.readLine();
                System.out.println("IN: " + s);
            }
        } catch (IOException x) {
            x.printStackTrace();
        }
    }
}
