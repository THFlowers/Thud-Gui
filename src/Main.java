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
    static Board board = new Board();
    static Player player = new Player(board);
    static PlayState playState = new PlayState();
    static RecordsManager recordsManager = new RecordsManager();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                UnsupportedLookAndFeelException ex)
        {
            System.err.println("Can't set look and feel");
        }

        player.initializeGame();
        playState.setTurn(BoardStates.DWARF);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

    }

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Thud!");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container pane = frame.getContentPane();

        /*
        JTextArea ta = new JTextArea(board.printBoard(false));
        pane.add(ta, BorderLayout.CENTER);
        */

        BoardDisplay display = new BoardDisplay(player, playState);
        pane.add(display, BorderLayout.CENTER);

        JMenuBar menu = new JMenuBar();
        pane.add(menu, BorderLayout.NORTH);

        JMenu fileMenu = new JMenu("Game");
        menu.add(fileMenu);

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                board = new Board();
                player = new Player(board);
                playState = new PlayState(BoardStates.TROLL, false);

                player.initializeGame();
                display.swapData(player, playState);
            }
        });
        fileMenu.add(newItem);

        JMenuItem saveItem = new JMenuItem("Save");
        fileMenu.add(saveItem);

        JMenuItem loadItem = new JMenuItem("Load");
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

        frame.pack();
        frame.setVisible(true);
    }
}
