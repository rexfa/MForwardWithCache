package com.bjcsc.pemc.rex.mf.admin;

import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Event;
import java.awt.BorderLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import java.awt.Point;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import java.awt.Rectangle;
import javax.swing.JPasswordField;
import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JScrollBar;

public class AdminConsolePad
{

	private JFrame jFrame = null;  //  @jve:decl-index=0:visual-constraint="10,10"
	private JPanel jContentPane = null;
	private JMenuBar jJMenuBar = null;
	private JMenu fileMenu = null;
	private JMenu editMenu = null;
	private JMenu helpMenu = null;
	private JMenuItem exitMenuItem = null;
	private JMenuItem aboutMenuItem = null;
	private JMenuItem cutMenuItem = null;
	private JMenuItem copyMenuItem = null;
	private JMenuItem pasteMenuItem = null;
	private JMenuItem saveMenuItem = null;
	private JDialog aboutDialog = null;  //  @jve:decl-index=0:visual-constraint="728,23"
	private JPanel aboutContentPane = null;
	private JLabel aboutVersionLabel = null;
	private JTextField jTxtHost = null;
	private JTextField jTxtPort = null;
	private JTextField jTxtName = null;
	private JPasswordField jPasswordAdmin = null;
	private JButton jBtnConncet = null;
	private JTree jTreeStats = null;
	private AdminClient ac  = null;  //  @jve:decl-index=0:
	private JButton jBtnTestTree = null;
	private JScrollPane jSPTreeScroll = null;
	private JCheckBox jCBUpdataEnble = null;
	private JButton jBtn_ShutdownServer = null;
	private JButton jBtn_StartServer = null;
	private JButton jBtn_RestartServer = null;
	private JCheckBox jCBCommandEnble = null;
	private JTextArea jTACMDResult = null;
	private JScrollPane jScrollPaneCMDR = null;
	private JButton jBtn_KillServer = null;
	/**
	 * This method initializes jTxtHost	
	 * 	
	 * @return javax.swing.TextField	
	 */
	private JTextField getJTxtHost()
	{
		if (jTxtHost == null)
		{
			jTxtHost = new JTextField();
			jTxtHost.setBounds(new Rectangle(10, 30, 110, 20));
			jTxtHost.setText("10.224.136.210");
		}
		return jTxtHost;
	}

	/**
	 * This method initializes jTxtPort	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTxtPort()
	{
		if (jTxtPort == null)
		{
			jTxtPort = new JTextField();
			jTxtPort.setBounds(new Rectangle(130, 30, 40, 20));
			jTxtPort.setText("12130");
		}
		return jTxtPort;
	}

	/**
	 * This method initializes jTxtName	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTxtName()
	{
		if (jTxtName == null)
		{
			jTxtName = new JTextField();
			jTxtName.setBounds(new Rectangle(180, 30, 60, 20));
			jTxtName.setText("admin");
		}
		return jTxtName;
	}

	/**
	 * This method initializes jPasswordAdmin	
	 * 	
	 * @return javax.swing.JPasswordField	
	 */
	private JPasswordField getJPasswordAdmin()
	{
		if (jPasswordAdmin == null)
		{
			jPasswordAdmin = new JPasswordField();
			jPasswordAdmin.setBounds(new Rectangle(250, 30, 60, 20));
			jPasswordAdmin.setText("123");
		}
		return jPasswordAdmin;
	}

	/**
	 * This method initializes jBtnConncet	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJBtnConncet()
	{
		if (jBtnConncet == null)
		{
			jBtnConncet = new JButton();
			jBtnConncet.setBounds(new Rectangle(330, 30, 90, 20));
			jBtnConncet.setText("Conncet");
			jBtnConncet.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					System.out.println("jBtnConncet actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
					
					String password = String.valueOf(jPasswordAdmin.getPassword()); 
					if(jTxtHost.getText().length()<7||jTxtPort.getText().length()<2||jTxtName.getText().length()<2||password.length()<2)
					{}
					else
					{
						if(ac!=null)
						{
							ac.SetStop(true);
							ac=null;				
						}
						ac = new AdminClient(jTxtHost.getText(),jTxtPort.getText(),jTxtName.getText(),password);
						ac.SetStatsTree(jTreeStats,jCBUpdataEnble,jSPTreeScroll);
						ac.SetLogTextArea(jTACMDResult);
						ac.SetBtnConncet(jBtnConncet);
						ac.start();
					}
				}
			});
		}
		return jBtnConncet;
	}

	/**
	 * This method initializes jTreeStats	
	 * 	
	 * @return javax.swing.JTree	
	 */
	private JTree getJTreeStats()
	{
		if (jTreeStats == null)
		{
			jTreeStats = new JTree(AdminClient.TreeModel);
        	jTreeStats.setShowsRootHandles(true);
        	jTreeStats.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);//给树定义选择模式为单选
        	

		}
		return jTreeStats;
	}

	/**
	 * This method initializes jBtnTestTree	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJBtnTestTree()
	{
		if (jBtnTestTree == null)
		{
			jBtnTestTree = new JButton();
			jBtnTestTree.setBounds(new Rectangle(555, 15, 110, 26));
			jBtnTestTree.setText("TestTree");
			jBtnTestTree.addMouseListener(new java.awt.event.MouseAdapter()
			{
				public void mouseClicked(java.awt.event.MouseEvent e)
				{
					System.out.println("mouseClicked()"); // TODO Auto-generated Event stub mouseClicked()
					TestTree();
				}
			});
		}
		return jBtnTestTree;
	}

	/**
	 * This method initializes jSPTreeScroll	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJSPTreeScroll()
	{
		if (jSPTreeScroll == null)
		{
			jSPTreeScroll = new JScrollPane();
			jSPTreeScroll.setBounds(new Rectangle(14, 60, 347, 657));
			jSPTreeScroll.setViewportView(getJTreeStats());
		}
		return jSPTreeScroll;
	}

	/**
	 * This method initializes jCBUpdataEnble	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCBUpdataEnble()
	{
		if (jCBUpdataEnble == null)
		{
			jCBUpdataEnble = new JCheckBox();
			jCBUpdataEnble.setBounds(new Rectangle(390, 68, 119, 21));
			jCBUpdataEnble.setText("Updata Enable");
			jCBUpdataEnble.setSelected(true);
		}
		return jCBUpdataEnble;
	}

	/**
	 * This method initializes jBtn_ShutdownServer	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJBtn_ShutdownServer()
	{
		if (jBtn_ShutdownServer == null)
		{
			jBtn_ShutdownServer = new JButton();
			jBtn_ShutdownServer.setBounds(new Rectangle(390, 185, 240, 28));
			jBtn_ShutdownServer.setText(AdminCommand.ShutdownServer);
			jBtn_ShutdownServer.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
					if(jCBCommandEnble.isSelected()&&ac!=null){
						if(!ac.sendCommand(AdminCommand.ShutdownServer))
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Send Command Failure!");
						}else
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Send Command Success!");
						}
						jCBCommandEnble.setSelected(false);
						}else
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Check Set!");
						}
						
				}
			});
		}
		return jBtn_ShutdownServer;
	}

	/**
	 * This method initializes jBtn_StartServer	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJBtn_StartServer()
	{
		if (jBtn_StartServer == null)
		{
			jBtn_StartServer = new JButton();
			jBtn_StartServer.setBounds(new Rectangle(390, 150, 240, 28));
			jBtn_StartServer.setText(AdminCommand.StartServer);
			jBtn_StartServer.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
					if(jCBCommandEnble.isSelected()&&ac!=null){
					if(!ac.sendCommand(AdminCommand.StartServer))
					{
						jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Send Command Failure!");
					}else
					{
						jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Send Command Success!");
					}
					jCBCommandEnble.setSelected(false);
					}else
					{
						jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Check Set!");
					}
						
				}
			});
		}
		return jBtn_StartServer;
	}

	/**
	 * This method initializes jBtn_RestartServer	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJBtn_RestartServer()
	{
		if (jBtn_RestartServer == null)
		{
			jBtn_RestartServer = new JButton();
			jBtn_RestartServer.setBounds(new Rectangle(390, 220, 240, 28));
			jBtn_RestartServer.setText(AdminCommand.RestartServer);
			jBtn_RestartServer.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
					if(jCBCommandEnble.isSelected()&&ac!=null){
						if(!ac.sendCommand(AdminCommand.RestartServer))
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Send Command Failure!");
						}else
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Send Command Success!");
						}
						jCBCommandEnble.setSelected(false);
						}else
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Check Set!");
						}
						
				}
			});
		}
		return jBtn_RestartServer;
	}

	/**
	 * This method initializes jCBCommandEnble	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCBCommandEnble()
	{
		if (jCBCommandEnble == null)
		{
			jCBCommandEnble = new JCheckBox();
			jCBCommandEnble.setBounds(new Rectangle(393, 119, 154, 26));
			jCBCommandEnble.setText("Command Enable");
			jCBCommandEnble.setSelected(false);
		}
		return jCBCommandEnble;
	}

	/**
	 * This method initializes jTACMDResult	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getJTACMDResult()
	{
		if (jTACMDResult == null)
		{
			jTACMDResult = new JTextArea();
		}
		return jTACMDResult;
	}

	/**
	 * This method initializes jScrollPaneCMDR	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPaneCMDR()
	{
		if (jScrollPaneCMDR == null)
		{
			jScrollPaneCMDR = new JScrollPane();
			jScrollPaneCMDR.setBounds(new Rectangle(390, 361, 269, 354));
			jScrollPaneCMDR.setViewportView(getJTACMDResult());
		}
		return jScrollPaneCMDR;
	}

	/**
	 * This method initializes jBtn_KillServer	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJBtn_KillServer()
	{
		if (jBtn_KillServer == null)
		{
			jBtn_KillServer = new JButton();
			jBtn_KillServer.setBounds(new Rectangle(390, 260, 240, 28));
			jBtn_KillServer.setText(AdminCommand.KillServer);
			jBtn_KillServer.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
					if(jCBCommandEnble.isSelected()&&ac!=null){
						if(!ac.sendCommand(AdminCommand.KillServer))
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Send Command Failure!");
						}else
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Send Command Success!");
						}
						jCBCommandEnble.setSelected(false);
						}else
						{
							jTACMDResult.setText(jTACMDResult.getText()+"\n\r"+"Check Set!");
						}
						
				}
			});
		}
		return jBtn_KillServer;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				AdminConsolePad application = new AdminConsolePad();
				application.getJFrame().setVisible(true);
			}
		});
	}

	/**
	 * This method initializes jFrame
	 * 
	 * @return javax.swing.JFrame
	 */
	private JFrame getJFrame()
	{
		if (jFrame == null)
		{
			jFrame = new JFrame();
			jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame.setJMenuBar(getJJMenuBar());
			jFrame.setSize(700, 800);
			jFrame.setContentPane(getJContentPane());
			jFrame.setTitle("Application");
		}
		return jFrame;
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane()
	{
		if (jContentPane == null)
		{
			jContentPane = new JPanel();
			jContentPane.setLayout(null);
			jContentPane.add(getJTxtHost(), null);
			jContentPane.add(getJTxtPort(), null);
			jContentPane.add(getJTxtName(), null);
			jContentPane.add(getJPasswordAdmin(), null);
			jContentPane.add(getJBtnConncet(), null);
			jContentPane.add(getJBtnTestTree(), null);
			jContentPane.add(getJSPTreeScroll(), null);
			jContentPane.add(getJCBUpdataEnble(), null);
			jContentPane.add(getJBtn_ShutdownServer(), null);
			jContentPane.add(getJBtn_StartServer(), null);
			jContentPane.add(getJBtn_RestartServer(), null);
			jContentPane.add(getJCBCommandEnble(), null);
			jContentPane.add(getJScrollPaneCMDR(), null);
			jContentPane.add(getJBtn_KillServer(), null);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jJMenuBar	
	 * 	
	 * @return javax.swing.JMenuBar	
	 */
	private JMenuBar getJJMenuBar()
	{
		if (jJMenuBar == null)
		{
			jJMenuBar = new JMenuBar();
			jJMenuBar.add(getFileMenu());
			jJMenuBar.add(getEditMenu());
			jJMenuBar.add(getHelpMenu());
		}
		return jJMenuBar;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getFileMenu()
	{
		if (fileMenu == null)
		{
			fileMenu = new JMenu();
			fileMenu.setText("File");
			fileMenu.add(getSaveMenuItem());
			fileMenu.add(getExitMenuItem());
		}
		return fileMenu;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getEditMenu()
	{
		if (editMenu == null)
		{
			editMenu = new JMenu();
			editMenu.setText("Edit");
			editMenu.add(getCutMenuItem());
			editMenu.add(getCopyMenuItem());
			editMenu.add(getPasteMenuItem());
		}
		return editMenu;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getHelpMenu()
	{
		if (helpMenu == null)
		{
			helpMenu = new JMenu();
			helpMenu.setText("Help");
			helpMenu.add(getAboutMenuItem());
		}
		return helpMenu;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getExitMenuItem()
	{
		if (exitMenuItem == null)
		{
			exitMenuItem = new JMenuItem();
			exitMenuItem.setText("Exit");
			exitMenuItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					System.exit(0);
				}
			});
		}
		return exitMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getAboutMenuItem()
	{
		if (aboutMenuItem == null)
		{
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setText("About");
			aboutMenuItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					JDialog aboutDialog = getAboutDialog();
					aboutDialog.pack();
					Point loc = getJFrame().getLocation();
					loc.translate(20, 20);
					aboutDialog.setLocation(loc);
					aboutDialog.setVisible(true);
				}
			});
		}
		return aboutMenuItem;
	}

	/**
	 * This method initializes aboutDialog	
	 * 	
	 * @return javax.swing.JDialog
	 */
	private JDialog getAboutDialog()
	{
		if (aboutDialog == null)
		{
			aboutDialog = new JDialog(getJFrame(), true);
			aboutDialog.setTitle("About");
			aboutDialog.setContentPane(getAboutContentPane());
		}
		return aboutDialog;
	}

	/**
	 * This method initializes aboutContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getAboutContentPane()
	{
		if (aboutContentPane == null)
		{
			aboutContentPane = new JPanel();
			aboutContentPane.setLayout(new BorderLayout());
			aboutContentPane.add(getAboutVersionLabel(), BorderLayout.CENTER);
		}
		return aboutContentPane;
	}

	/**
	 * This method initializes aboutVersionLabel	
	 * 	
	 * @return javax.swing.JLabel	
	 */
	private JLabel getAboutVersionLabel()
	{
		if (aboutVersionLabel == null)
		{
			aboutVersionLabel = new JLabel();
			aboutVersionLabel.setText("Version 1.0");
			aboutVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		}
		return aboutVersionLabel;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getCutMenuItem()
	{
		if (cutMenuItem == null)
		{
			cutMenuItem = new JMenuItem();
			cutMenuItem.setText("Cut");
			cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
					Event.CTRL_MASK, true));
		}
		return cutMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getCopyMenuItem()
	{
		if (copyMenuItem == null)
		{
			copyMenuItem = new JMenuItem();
			copyMenuItem.setText("Copy");
			copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
					Event.CTRL_MASK, true));
		}
		return copyMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getPasteMenuItem()
	{
		if (pasteMenuItem == null)
		{
			pasteMenuItem = new JMenuItem();
			pasteMenuItem.setText("Paste");
			pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
					Event.CTRL_MASK, true));
		}
		return pasteMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getSaveMenuItem()
	{
		if (saveMenuItem == null)
		{
			saveMenuItem = new JMenuItem();
			saveMenuItem.setText("Save");
			saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
					Event.CTRL_MASK, true));
		}
		return saveMenuItem;
	}
	
	private void TestTree()
	{
		if(jTxtHost.getText().length()<7||jTxtPort.getText().length()<2||jTxtName.getText().length()<2)
		{}
		else
		{
			if(ac!=null)
			{
				ac.SetStop(true);
				ac=null;				
			}
			ac = new AdminClient(jTxtHost.getText(),jTxtPort.getText(),jTxtName.getText(),"123");
			ac.SetStatsTree(jTreeStats,jCBUpdataEnble,jSPTreeScroll);
			//ac.start();
			ac.testTree();
		}
	}

}
