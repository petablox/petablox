// SolverGUI.java, created Jun 18, 2004 12:02:08 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileFilter;

/**
 * SolverGUI
 * 
 * @author John Whaley
 * @version $Id: SolverGUI.java 531 2005-04-29 06:39:10Z joewhaley $
 */
public class SolverGUI extends JFrame {
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 4049917160464004665L;
    
    private javax.swing.JPanel jContentPane = null;
    private JMenuBar jJMenuBar = null;
    private JMenu jFileMenu = null;
    private JMenuItem jOpenMenuItem = null;
    private JMenuItem jExitMenuItem = null;
    private JFileChooser jFileChooser = null; //  @jve:decl-index=0:visual-constraint="334,13"
    private JTextPane jTextPane = null;
    private JMenuItem jSaveMenuItem = null;
    private JMenuItem jSaveAsMenuItem = null;
    private JPanel jPanel = null;
    private JTextField jTextField = null;
    private JButton jButton = null;
    private FileFilter jFileFilter = null;
    private JScrollPane jScrollPane = null;

    /**
     * This method initializes jJMenuBar
     * 
     * @return javax.swing.JMenuBar
     */
    private JMenuBar getJJMenuBar() {
        if (jJMenuBar == null) {
            jJMenuBar = new JMenuBar();
            jJMenuBar.add(getJFileMenu());
        }
        return jJMenuBar;
    }

    /**
     * This method initializes jFileMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getJFileMenu() {
        if (jFileMenu == null) {
            jFileMenu = new JMenu();
            jFileMenu.setText("File");
            jFileMenu.add(getJOpenMenuItem());
            jFileMenu.add(getJSaveMenuItem());
            jFileMenu.add(getJSaveAsMenuItem());
            jFileMenu.addSeparator();
            jFileMenu.add(getJExitMenuItem());
        }
        return jFileMenu;
    }

    /**
     * This method initializes jOpenMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getJOpenMenuItem() {
        if (jOpenMenuItem == null) {
            jOpenMenuItem = new JMenuItem();
            jOpenMenuItem.setText("Open Datalog...");
            jOpenMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // TODO: If buffer modified, query for save.
                    JFileChooser fc = getJFileChooser();
                    int returnVal = fc.showOpenDialog(SolverGUI.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        try {
                            jTextPane.setPage(file.toURL());
                        } catch (MalformedURLException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    } else {
                        // Open command cancelled by user.
                    }
                }
            });
        }
        return jOpenMenuItem;
    }

    private static void createAndShowGUI() {
        SolverGUI t = new SolverGUI();
        t.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        t.setVisible(true);
    }

    /**
     * This method initializes jExitMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getJExitMenuItem() {
        if (jExitMenuItem == null) {
            jExitMenuItem = new JMenuItem();
            jExitMenuItem.setText("Exit");
            jExitMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.exit(0);
                }
            });
        }
        return jExitMenuItem;
    }

    /**
     * This method initializes jFileFilter
     * 
     * @return javax.swing.FileFilter
     */
    private FileFilter getJFileFilter() {
        if (jFileFilter == null) {
            jFileFilter = new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith(".datalog");
                }

                public String getDescription() {
                    return "Datalog files (.datalog)";
                }
            };
        }
        return jFileFilter;
    }

    /**
     * This method initializes jFileChooser
     * 
     * @return javax.swing.JFileChooser
     */
    private JFileChooser getJFileChooser() {
        if (jFileChooser == null) {
            jFileChooser = new JFileChooser();
            jFileChooser.setFileFilter(getJFileFilter());
        }
        return jFileChooser;
    }

    /**
     * This method initializes jTextPane
     * 
     * @return javax.swing.JTextPane
     */
    private JTextPane getJTextPane() {
        if (jTextPane == null) {
            jTextPane = new JTextPane();
            jTextPane.setFont(new java.awt.Font("Times New Roman", java.awt.Font.ITALIC, 16));
        }
        return jTextPane;
    }

    /**
     * This method initializes jSaveMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getJSaveMenuItem() {
        if (jSaveMenuItem == null) {
            jSaveMenuItem = new JMenuItem();
            jSaveMenuItem.setText("Save");
        }
        return jSaveMenuItem;
    }

    /**
     * This method initializes jSaveAsMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getJSaveAsMenuItem() {
        if (jSaveAsMenuItem == null) {
            jSaveAsMenuItem = new JMenuItem();
            jSaveAsMenuItem.setText("Save As...");
            jSaveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    JFileChooser fc = getJFileChooser();
                    int returnVal = fc.showSaveDialog(SolverGUI.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        jTextPane.setText("Saving: " + file.getName());
                    } else {
                        jTextPane.setText("Save command cancelled by user.");
                    }
                }
            });
        }
        return jSaveAsMenuItem;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.setLayout(new BorderLayout());
            jPanel.add(getJTextField(), java.awt.BorderLayout.CENTER);
            jPanel.add(getJButton(), java.awt.BorderLayout.EAST);
        }
        return jPanel;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getJTextField() {
        if (jTextField == null) {
            jTextField = new JTextField();
            jTextField.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // Add Datalog rule.
                    System.out.println("actionPerformed()"); // TODO
                    // Auto-generated
                    // Event stub
                    // actionPerformed()
                }
            });
        }
        return jTextField;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setText("Add");
            jButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    File sound = new File("bddbddb.wav");
                    try {
                        AudioClip audioClip = Applet.newAudioClip(sound.toURL());
                        audioClip.play();
                    } catch (MalformedURLException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            });
        }
        return jButton;
    }

    /**
     * 
     * This method initializes jScrollPane
     * 
     * 
     * 
     * @return javax.swing.JScrollPane
     *  
     */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getJTextPane());
        }
        return jScrollPane;
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    /**
     * This is the default constructor
     */
    public SolverGUI() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     */
    private void initialize() {
        this.setTitle("bddbddb");
        this.setSize(300, 200);
        this.setJMenuBar(getJJMenuBar());
        this.setContentPane(getJContentPane());
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private javax.swing.JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout(new java.awt.BorderLayout());
            jContentPane.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
            jContentPane.add(getJPanel(), java.awt.BorderLayout.SOUTH);
        }
        return jContentPane;
    }
}