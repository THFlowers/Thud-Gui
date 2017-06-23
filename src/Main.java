import thud.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * Created by Thai Flowers on 6/10/2017.
 */
public class Main {
    static Board board;
    static Player player;
    static PlayState playState;
    static RecordsManager recordsManager;

    static JFrame frame;
    static Container pane;
    static BoardDisplay display;
    static JMenuBar menu;
    static JMenu fileMenu;
    static JMenuItem newItem;
    static JMenuItem saveItem;
    static JMenuItem loadItem;
    static JMenu actionMenu;
    static JMenuItem forfeit;
    static JMenuItem removeAll;
    static JMenuItem removeNone;
    static BoardStatusBar status;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                UnsupportedLookAndFeelException ex)
        {
            System.err.println("Can't set look and feel");
        }

        board  = new Board();
        player = new Player(board);
        playState = player.initializeGame();
        recordsManager = new RecordsManager();
        recordsManager.addRound(player);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

    }

    public static void createAndShowGUI() {
        frame = new JFrame("Thud!");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pane = frame.getContentPane();

        display = new BoardDisplay(player, playState);
        pane.add(display, BorderLayout.CENTER);

        menu = new JMenuBar();
        pane.add(menu, BorderLayout.NORTH);

        fileMenu = new JMenu("Game");
        menu.add(fileMenu);

        // human v human
        newItem = new JMenuItem("New");
        newItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                board = new Board();
                player = new Player(board);
                recordsManager = new RecordsManager();

                playState = player.initializeGame();
                recordsManager.addRound(player);

                display.swapData(player, playState);

                status.setLeft("New Game");
                status.setRight("Dwarfs play first");
            }
        });
        fileMenu.add(newItem);

        saveItem = new JMenuItem("Save");
        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        fileMenu.add(saveItem);

        loadItem = new JMenuItem("Load");
        loadItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Open game records");
                chooser.setMultiSelectionEnabled(false);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String filePath = chooser.getSelectedFile().getAbsolutePath();
                    board = new Board();
                    player = new Player(board);
                    player.initializeGame();
                    playState = new PlayState();

                    try {
                        recordsManager.loadFile(filePath);
                    }
                    catch (IOException ex) {
                        System.err.println("Failed to load file");
                    }

                    recordsManager.replayRecords(player, playState);
                    display.swapData(player, playState);
                }
            }
        });
        fileMenu.add(loadItem);

        actionMenu = new JMenu("Action");
        menu.add(actionMenu);

        forfeit = new JMenuItem("Forfeit Round");
        forfeit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (recordsManager.getCurrentRound() == 2) {
                    player.calculateScores(recordsManager.getCurrentRound());

                    int[] scores = player.getScores();
                    String message;
                    if (scores[0] > scores[1])
                        message = "Player 1";
                    else if (scores[1] > scores[0])
                        message = "Player 2 Wins";
                    else
                        message = "Draw";

                    status.setLeft("Game Over");
                    status.setRight(message);

                    display.lock();
                }
                else {
                    player.calculateScores(recordsManager.getCurrentRound());
                    board = new Board();
                    player = new Player(board);

                    playState = player.initializeGame();
                    recordsManager.addRound(player);

                    display.swapData(player, playState);

                    status.setLeft("Round 2");
                    status.setRight("Dwarfs play first");
                }
            }
        });
        actionMenu.add(forfeit);

        removeAll = new JMenuItem("Remove All");
        removeAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        actionMenu.add(removeAll);

        removeNone = new JMenuItem("Remove None");
        removeNone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        actionMenu.add(removeNone);

        status = new BoardStatusBar();
        display.setStatusBar(status);
        status.setOpaque(false);
        status.setLeft("Welcome to Thud!");
        status.setRight("Dwarfs move first");
        frame.add(status, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
    }
}
