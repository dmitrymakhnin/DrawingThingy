import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;

public class Sample
{
    public static void main(String[] args)
    {
        JFrame f = new JFrame("A Console");
        MyPanel p = new MyPanel();
        f.add(p);
        f.setResizable(false);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

    private static class MyPanel extends JPanel implements Runnable
    {
        public static int ROWS = 24; 
        public static int COLUMNS = 80;
        private int cur_row = 0;
        private int cur_col = 0;               
        
        /* row major character buffer */
        public char char_buffer[][] = null;
        public Color fg_color_buffer[][] = null;
        public Color bg_color_buffer[][] = null;
        public BufferedImage surface = null;
        public Font f = null;
        Graphics sg = null;
        public int h;
        public int w;
        public int font_h;
        public int font_w;
        public boolean blink = false;
        public boolean isCtrlPressed = false;

        public MyPanel()
        {
            f = new Font("Courier New", Font.PLAIN, 13);
            FontMetrics fm = getFontMetrics(f);
            font_h = fm.getHeight();
            font_w = fm.stringWidth("|");
            h = (font_h) * ROWS;
            w = (font_w) * COLUMNS; 
            System.out.println("size: " + w + "x" + h + "."); 
            setPreferredSize(new Dimension(w, h)); 

            char_buffer = new char[ROWS][COLUMNS]; 
            fg_color_buffer = new Color[ROWS][COLUMNS];
            bg_color_buffer = new Color[ROWS][COLUMNS];

            for (int i = 0; i < ROWS; ++i) {
                for (int j = 0; j < COLUMNS; ++j) {
                    char_buffer[i][j] = 0;
                    fg_color_buffer[i][j] = Color.GREEN;
                    bg_color_buffer[i][j] = Color.BLACK;
                }
            }    

            surface = new BufferedImage ( 
                w, 
                h, 
                BufferedImage.TYPE_INT_ARGB
            );
            sg = surface.getGraphics();
            new Thread(this).start();

            setFocusable(true);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent evt) {
                    isCtrlPressed = false;  
                }

                @Override
                public void keyPressed(KeyEvent evt) {
                    switch (evt.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                        cur_col = 0; /* cr */
                        ++cur_row;   /* lf */
                        break;

                    case KeyEvent.VK_SPACE:
                        ++cur_col;
                        if (cur_col == COLUMNS) {
                            cur_col = 0;
                            ++cur_row;
                        }
                        break;

                    case KeyEvent.VK_CONTROL:
                        isCtrlPressed = true; 
                        break;

                    case KeyEvent.VK_C:
                        if (isCtrlPressed) {
                            for (int i = 0; i < ROWS; ++i) {
                                for (int j = 0; j < COLUMNS; ++j) {
                                    char_buffer[i][j] = 0;
                                    fg_color_buffer[i][j] = Color.GREEN;
                                    bg_color_buffer[i][j] = Color.BLACK;
                                    cur_row = 0;
                                    cur_col = 0;
                                } 
                            } 
                        }
                        break;
                    
                    case KeyEvent.VK_S:
                        if (isCtrlPressed) {
                            String name = UUID.randomUUID().toString() + ".txt";                            
                            System.out.println("Writing to " + name + ".");
                            try {
                                File f = new File(name);
                                FileOutputStream fos = new FileOutputStream(f);
                                BufferedOutputStream fw = new BufferedOutputStream(fos);
                                for (int i = 0; i < ROWS; ++i) {
                                    for (int j = 0; j < COLUMNS; ++j) {
                                        char c = char_buffer[i][j];
                                        if (!(c >= ' ' && c <= 'z')) c = ' '; 
                                        System.out.println("printing '" + c + "'");
                                        fw.write(c);
                                        if (j == COLUMNS - 1) {
                                            fw.write('\r');
                                            fw.write('\n');
                                        }
                                    }
                                }
                                fw.flush();
                                fw.close();
                            } catch (Exception e){}       
                        }
                        break;
                    case KeyEvent.VK_UP:
                        cur_row = cur_row - 1 == -1 ? 0 : cur_row - 1;
                        break;
                    case KeyEvent.VK_DOWN:
                        cur_row = cur_row + 1 == ROWS ? cur_row : cur_row + 1; 
                        break;
                    case KeyEvent.VK_LEFT:
                        cur_col = cur_col - 1 == -1 ? 0 : cur_col - 1;
                        break;
                    case KeyEvent.VK_RIGHT:
                        cur_col = cur_col + 1 == COLUMNS ? cur_col : cur_col + 1; 
                        break;
                    };
                }

                @Override
                public void keyTyped(KeyEvent evt) {
                    char c = evt.getKeyChar();
                    Character.UnicodeBlock block = Character.UnicodeBlock.of(c);

                    if (!Character.isISOControl(c) &&
                        (c != KeyEvent.CHAR_UNDEFINED) &&
                        block != null &&
                        block != Character.UnicodeBlock.SPECIALS &&
                        c != ' ') {
                        char_buffer[cur_row][cur_col] = evt.getKeyChar();
                        ++cur_col;
                        if (cur_col == COLUMNS) {
                            cur_col = 0;
                            ++cur_col; 
                        }
                    }
                }
            });
             
            new Thread(new Runnable() {
                @Override
                public void run() {                    
                    while (true) {
                        repaint();
                        try {
                            Thread.sleep(1000 / 60);
                        } catch(Exception e){}
                    }
                }
            }).start();
        }

        public void paint(Graphics g)
        {
            for (int i = 0; i < ROWS; ++i) {
                for (int j = 0; j < COLUMNS; ++j) {
                    if (blink && i == cur_row && j == cur_col) {
                        sg.setColor(new Color(0x66, 0x66, 0x66));
                    } else {
                        sg.setColor(bg_color_buffer[i][j]);
                    }
                    int x_base = j * font_w;
                    int y_base = i * font_h;
                    int x_bottom = x_base + font_w;
                    int y_bottom = y_base + font_h;
                    sg.fillRect(x_base, y_base, x_bottom, y_bottom);
                    if (blink && i == cur_row && j == cur_col) {
                        sg.setColor(new Color(0x20, 0xff, 0x20));
                    } else {
                        sg.setColor(fg_color_buffer[i][j]);
                    }
                    sg.setFont(f);
                    if (char_buffer[i][j] == 0) {
                        sg.drawString(" ", x_base, y_bottom);
                    } else {
                        sg.drawString("" + char_buffer[i][j], x_base, y_bottom - 3);
                    }
                }
            }    
            g.drawImage(surface, 0, 0, null);    
        }

        @Override
        public void run() 
        {
            while (true) {
                blink = !blink;
                try {
                    Thread.sleep(1000);
                } catch(Exception e){}
            }
        }
    }
}