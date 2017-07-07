import thud.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by Thai Flowers on 6/10/2017.
 */
public class Main {
	static Board board;
	static Player player;
	static PlayState playState;
	static RecordsManager recordsManager;
	static MonteCarloPlay ai;

	static JFrame frame;
	static Container pane;
	static BoardDisplay display;
	static JMenuBar menu;

	static JMenu fileMenu;
	static JMenuItem newItem;
	static JMenuItem newAIItem;
	static JMenuItem saveItem;
	static JMenuItem loadItem;

	static JMenu actionMenu;
	static JMenuItem forfeit;
	static JMenuItem removeAll;
	static JMenuItem removeNone;

	static JMenu aboutMenu;
	static JMenuItem thudItem;
	static JMenuItem copyrightItem;
	static JMenuItem howToPlayItem;

	static BoardStatusBar status;

	public static void main(String[] args) {
		board = new Board();
		player = new Player(board);
		playState = player.initializeGame();
		recordsManager = new RecordsManager();
		recordsManager.addRound(player);

		javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
	}

	public static void createAndShowGUI() {
		frame = new JFrame("Thud!");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setMinimumSize(new Dimension(400, 400));

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				savePrompt(() -> {
					frame.dispose();
					System.exit(0);
				});
			}
		});

		pane = frame.getContentPane();

		display = new BoardDisplay(player, playState);
		pane.add(display, BorderLayout.CENTER);

		menu = new JMenuBar();
		pane.add(menu, BorderLayout.NORTH);

		fileMenu = new JMenu("Game");
		menu.add(fileMenu);

		// human v human
		newItem = new JMenuItem("New");
		newItem.addActionListener(e ->
				savePrompt(() -> {

					board = new Board();
					player = new Player(board);
					recordsManager = new RecordsManager();

					playState = player.initializeGame();
					recordsManager.addRound(player);

					display.swapData(player, playState);

					status.setLeft("New Game");
					status.setRight("Dwarfs play first");

					ai = null;
					display.setAI(null);

					removeNone.setEnabled(false);
					removeAll.setEnabled(false);
					forfeit.setEnabled(false);
				})
		);
		fileMenu.add(newItem);

		newAIItem = new JMenuItem("New AI Game");
		newAIItem.addActionListener(e ->
				savePrompt(() -> {

					board = new Board();
					player = new Player(board);
					recordsManager = new RecordsManager();

					playState = player.initializeGame();
					recordsManager.addRound(player);
					ai = new MonteCarloPlay(BoardStates.TROLL);

					display.swapData(player, playState);
					display.setAI(ai);

					status.setLeft("New AI Game");
					status.setRight("Human plays first");

					removeNone.setEnabled(false);
					removeAll.setEnabled(false);
					forfeit.setEnabled(false);
				})
		);
		fileMenu.add(newAIItem);

		saveItem = new JMenuItem("Save");
		saveItem.addActionListener(e -> {
			if (!player.getMoveLog().isEmpty() || recordsManager.getCurrentRound() > 1)
				saveDialog();
			else {
				JOptionPane.showMessageDialog(frame.getComponent(0), "Nothing to Save!", "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		fileMenu.add(saveItem);

		loadItem = new JMenuItem("Load");
		loadItem.addActionListener(e ->
				savePrompt(() -> {
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle("Open game records");
					chooser.setMultiSelectionEnabled(false);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setFileFilter(new FileNameExtensionFilter("Plain text", "txt", "text"));

					int ret = chooser.showOpenDialog(null);
					if (ret == JFileChooser.APPROVE_OPTION) {
						String filePath = chooser.getSelectedFile().getAbsolutePath();
						board = new Board();
						player = new Player(board);
						player.initializeGame();
						playState = new PlayState();

						try {
							recordsManager.loadFile(filePath);
						} catch (IOException ex) {
							System.err.println("Failed to load file");
						}

						// replayRecords clears resumeRound, need current value later
						boolean resumeRound = recordsManager.resumeRound();
						recordsManager.replayRecords(player, playState);

						boolean isRemoveTurn = playState.isRemoveTurn();
						boolean mustRemove = player.mustRemove();

						removeAll.setEnabled(isRemoveTurn);
						removeNone.setEnabled(isRemoveTurn && !mustRemove);

						if (!resumeRound) {
							int round = recordsManager.getCurrentRound();
							forfeit.setEnabled(false);
							if (round == 1) {

								playState = player.initializeGame();
								recordsManager.addRound(player);

								status.setLeft("New Game");
								status.setRight("Dwarfs play first");
							}
							if (round == 2) {

								playState = player.initializeGame();
								recordsManager.addRound(player);

								status.setLeft("Round 2");
								status.setRight("Dwarfs play first");
							}
							if (round == 3) {
								setGameOver();
							}
						} else {
							forfeit.setEnabled(!mustRemove);
							status.setLeft(display.getDefaultLeftStatusString() + player.getLastMove());
							status.setRight(display.getDefaultRightStatusString());
						}

						display.swapData(player, playState);
						display.setAI(null);
						ai = null;
					}
				})
		);
		fileMenu.add(loadItem);

		actionMenu = new JMenu("Action");
		menu.add(actionMenu);

		forfeit = new JMenuItem("Forfeit Round");
		forfeit.addActionListener(e -> {
			player.calculateScores(recordsManager.getCurrentRound());
			if (recordsManager.getCurrentRound() == 2) {
				setGameOver();
			} else {
				board = new Board();
				player = new Player(board);

				playState = player.initializeGame();
				recordsManager.addRound(player);

				display.swapData(player, playState);

				removeNone.setEnabled(false);
				removeAll.setEnabled(false);

				if (ai == null) {
					status.setLeft("Round 2");
					status.setRight("Dwarfs play first");

					forfeit.setEnabled(false);
				} else {
					ai = new MonteCarloPlay(BoardStates.DWARF);
					display.setAI(ai);

					forfeit.setEnabled(false);

					display.lock();
					status.setLeft("Dwarfs Move First");
					status.setRight("AI Thinking");
					display.repaint();

					SwingUtilities.invokeLater(() -> {
						// no remove possible on first turn (under normal rules)
						String move = ai.selectPlay();
						player.play(playState, move);

						status.setLeft(display.getDefaultLeftStatusString() + " " + move);
						status.setRight(display.getDefaultRightStatusString());

						forfeit.setEnabled(true);
						display.repaint();
						display.unlock();
					});
				}
			}
		});
		actionMenu.add(forfeit);

		display.setForfeitButton(forfeit);

		removeAll = new JMenuItem("Remove All");
		removeAll.addActionListener(e -> {
			String oldMove = player.getLastMove();
			String[] oldCommand = oldMove.split(" ");
			BoardPoint pos = new BoardPoint(oldCommand[2]);

			PossiblePieceMoves possible = player.getPossiblePieceMoves(playState, pos);
			StringBuilder remPositions = new StringBuilder();
			for (BoardPoint remPos : possible.getRemove()) {
				remPositions.append(remPos.toString());
				remPositions.append(" ");
			}

			String move = String.format("R %s", remPositions.toString());
			player.play(playState, move);
			removeButtonPlay(move);
		});
		actionMenu.add(removeAll);

		removeNone = new JMenuItem("Remove None");
		removeNone.addActionListener(e -> {
			String move = "R ";
			player.play(playState, move);
            removeButtonPlay(move);
		});
		actionMenu.add(removeNone);

		display.setRemoveButtons(removeAll, removeNone);

		aboutMenu = new JMenu("About");
		menu.add(aboutMenu);

		thudItem = new JMenuItem("Thud?");
		String thudMessage =
				"Thud! is an abstract asymmetric board game similar to ancient Hnefatafl. \n" +
						"The game is a battle between Trolls (8 inner pieces) and Dwarfs (32 outer pieces). \n" +
						"It is played in 2 rounds, with each player taking one turn as each side. \n " +
						"A round ends by mutual agreement to \"Forfeit\", then the score for the round is calculated. \n" +
						"Each surviving Troll is 40 points, and each Dwarf is 10. \n" +
						"See https://en.wikipedia.org/wiki/Games_of_the_Discworld#Thud for more info. \n" +
						"See \"How To Play\" for piece movement";
		thudItem.addActionListener(e ->
				JOptionPane.showMessageDialog(frame.getComponent(0), thudMessage, "Title", JOptionPane.INFORMATION_MESSAGE));
		aboutMenu.add(thudItem);

		copyrightItem = new JMenuItem("Copyright");
		String copyrightMessage =
				"Thud! © Trevor Truran \n" +
						"Thud-Gui © 2017 Thai Flowers released under the GNU Public License v3";
		copyrightItem.addActionListener(e ->
				JOptionPane.showMessageDialog(frame.getComponent(0), copyrightMessage, "Thud-Gui Copyright", JOptionPane.INFORMATION_MESSAGE));
		aboutMenu.add(copyrightItem);

		howToPlayItem = new JMenuItem("How To Play");
		String howToPlayMessage =
				"Dwarfs play first. \n" +
						"Trolls move like chess kings and MAY remove Dwarfs 1 square away after moving.\n " +
						"Click and drag to capture one piece, select the menu item for None or All adjacent.\n " +
						"Dwarfs move like chess queens, and can only capture lining up and hurling themselves at Trolls. \n" +
						"The legal maximum distance being the number of Dwarfs in a row. \n" +
						"Likewise Trolls can shove, but must land next to a dwarf and MUST capture at least one Dwarf. \n" +
						"Forfeit is not allowed when a capture is needed. \n" +
						"The thud stone does not move.";
		howToPlayItem.addActionListener(e ->
				JOptionPane.showMessageDialog(frame.getComponent(0), howToPlayMessage, "How to Play", JOptionPane.INFORMATION_MESSAGE));
		aboutMenu.add(howToPlayItem);

		status = new BoardStatusBar();
		display.setStatusBar(status);
		status.setOpaque(false);
		status.setLeft("Welcome to Thud!");
		status.setRight("Dwarfs play first");
		frame.add(status, BorderLayout.SOUTH);

		frame.pack();
		frame.setVisible(true);
	}

	static void setGameOver() {
		int[] scores = player.getScores();
		String message;
		if (scores[0] > scores[1])
			message = (ai == null) ? "Player 1 Wins" : "Player Wins";
		else if (scores[1] > scores[0])
			message = (ai == null) ? "Player 2 Wins" : "Computer Wins";
		else
			message = "Draw";

		status.setLeft("Game Over");
		status.setRight(message);

		display.lock();
		forfeit.setEnabled(false);
	}

	static void saveDialog() {
		// Confirm dialog code lifted from: Roberto Luis Bisbé
		// https://stackoverflow.com/questions/3651494/jfilechooser-with-confirmation-dialog
		JFileChooser chooser = new JFileChooser() {
			@Override
			public void approveSelection() {
				File f = getSelectedFile();
				if (f.exists() && getDialogType() == SAVE_DIALOG) {
					int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);
					switch (result) {
						case JOptionPane.YES_OPTION:
							super.approveSelection();
							return;
						case JOptionPane.NO_OPTION:
							return;
						case JOptionPane.CLOSED_OPTION:
							return;
						case JOptionPane.CANCEL_OPTION:
							cancelSelection();
							return;
					}
				}
				super.approveSelection();
			}
		};
		chooser.setDialogTitle("Save game records");
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setFileFilter(new FileNameExtensionFilter("Plain text", "txt", "text"));

		int ret = chooser.showSaveDialog(null);
		if (ret == JFileChooser.APPROVE_OPTION) {
			String filePath = chooser.getSelectedFile().getAbsolutePath();

			try {
				recordsManager.saveFile(filePath);
			} catch (IOException ex) {
				System.err.println("Failed to save file");
			}
		}
	}

	static void savePrompt(final Runnable command) {
		int round = recordsManager.getCurrentRound();
		if (round > 1 || (round == 1 && player.getMoveLog().size() > 0)) {

			int result = JOptionPane.showConfirmDialog(frame.getComponent(0), "Game in progress, do you want to save?", "Save?", JOptionPane.YES_NO_CANCEL_OPTION);
			switch (result) {
				case JOptionPane.YES_OPTION:
					saveDialog();
					command.run();
					return;
				case JOptionPane.NO_OPTION:
					command.run();
					return;
				case JOptionPane.CLOSED_OPTION:
				case JOptionPane.CANCEL_OPTION:
					return;
			}
		} else
			command.run();
	}

	static void removeButtonPlay(String move) {
		if (ai != null) {
			display.lock();
			status.setLeft(display.getDefaultLeftStatusString() + move);
			status.setRight("AI Thinking");

			SwingUtilities.invokeLater(() -> {
				ai.opponentPlay(move);
				String aiMove = ai.selectPlay();
				player.play(playState, aiMove);
				if (playState.isRemoveTurn())
					player.play(playState, ai.selectPlay());

				status.setLeft(display.getDefaultLeftStatusString() + " " + aiMove);
				status.setRight(display.getDefaultRightStatusString());

				forfeit.setEnabled(true);
				display.repaint();
				display.unlock();
			});
		} else {
			status.setLeft(display.getDefaultLeftStatusString() + " " + move);
			status.setRight(display.getDefaultRightStatusString());
			forfeit.setEnabled(true);
		}

		removeNone.setEnabled(false);
		removeAll.setEnabled(false);
		display.clearMouseData();
		display.repaint();
	}
}
