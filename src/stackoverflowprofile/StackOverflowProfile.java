package stackoverflowprofile;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

public final class StackOverflowProfile extends JFrame {

    int PROFILE_VIEW_TIME = 5000;
    ProfileUpdater profileupdater;
    Wrapper wrapper = new Wrapper();
    ResourceBundle res = ResourceBundle.getBundle("resources");
    Border yellowborder = BorderFactory.createLineBorder(Color.YELLOW);
    Border redborder = BorderFactory.createLineBorder(Color.RED);
    Border greenborder = BorderFactory.createLineBorder(Color.GREEN);
    Image iconimage = null;
    JLabel screen = null;

    MouseAdapter mouseAdapter = new MouseAdapter() {

        @Override
        public void mouseClicked(MouseEvent e) {
            showInBrowser();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            wrapper.mousein = true;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            wrapper.mousein = false;
        }
    };

    StackOverflowProfile() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setSize(208, 58);
        Rectangle bound = java.awt.GraphicsEnvironment.
                getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setLocation((bound.width - getWidth()) / 2, 0);

        try {
            URL iconurl = StackOverflowProfile.class.getResource("iconimage.png");
            iconimage = javax.imageio.ImageIO.read(iconurl);
            setIconImage(iconimage);
        } catch (IOException ex) {
            System.out.println("Error while setting icon.");
        }

        File imagefile = new File("afzalex.png");
        ImageIcon oldicon;
        try {
            wrapper.currimage = javax.imageio.ImageIO.read(imagefile);
            oldicon = new ImageIcon(wrapper.currimage);
        } catch (IOException ex) {
            oldicon = new ImageIcon();
        }
        setTray();
        screen = new JLabel(oldicon);
        screen.setBorder(yellowborder);
        screen.addMouseListener(mouseAdapter);
        screen.addMouseMotionListener(mouseAdapter);
        add(screen);
        updateProfile(wrapper);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showProfile(wrapper, PROFILE_VIEW_TIME);
        Timer timer = new Timer(900_000, (e) -> {
            updateProfile(wrapper);
        });
        timer.start();
    }

    public final void showInBrowser() {
        try {
            Desktop desk = Desktop.getDesktop();
            URI browse = new URI(res.getString("browseloc"));
            desk.browse(browse);
            dispose();
            wrapper.ishidden = true;
        } catch (IOException | URISyntaxException ex) {
            System.out.println("Error in browsing : " + ex);
        }
    }

    public final void showProfile(Wrapper wrapper, long time) {
        if (wrapper.ishidden && !wrapper.isclosed) {
            new ShowProfile(wrapper, time).start();
        }
    }

    public final void updateProfile(Wrapper wrapper) {
        if (profileupdater == null || !profileupdater.isAlive()) {
            profileupdater = new ProfileUpdater(wrapper) {
                @Override
                public void onDownloadComplete(Image newimage) {
                    screen.setIcon(new ImageIcon(newimage));
                    wrapper.currimage = newimage;
                    System.out.println("Image downloaded successfully.");
                }

                @Override
                public void onDownloadFail() {
                    System.out.println("Error while downloading image.  ");
                    screen.setBorder(redborder);
                }

                @Override
                public void onUpdateComplete() {
                    System.out.println("Image is updated successfully.");
                    screen.setBorder(greenborder);
                }

                @Override
                public void onUpdateFail() {
                    screen.setBorder(redborder);
                }
            };
            wrapper.resetToDownload();
            screen.setBorder(yellowborder);
            profileupdater.start();
            showProfile(wrapper, PROFILE_VIEW_TIME);
        }
    }

    public void close() {
        dispose();
        wrapper.isclosed = true;
        wrapper.ishidden = true;
        setVisible(false);
        System.out.println("closing");
        System.exit(0);
    }

    class ShowProfile extends Thread {

        private long time;
        private Wrapper wrapper;

        ShowProfile(Wrapper wrapper, long time) {
            this.wrapper = wrapper;
            this.time = time;
        }

        @Override
        public void run() {
            boolean flag = true;
            wrapper.mousein = false;
            wrapper.ishidden = false;
            StackOverflowProfile.this.setVisible(true);
            while (flag) {
                try {
                    Thread.sleep(time);
                    while (true) {
                        if (wrapper.mousein) {
                            Thread.sleep(1000);
                            continue;
                        } else {
                            break;
                        }
                    }
                    if (!wrapper.ishidden) {
                        wrapper.ishidden = true;
                        StackOverflowProfile.this.setVisible(false);
                        wrapper.mousein = false;
                    }
                    wrapper.todownload = false;
                    flag = false;
                } catch (InterruptedException ex) {
                    System.out.println("Error in hiding : " + ex);
                    flag = true;
                }
            }
        }
    }

    abstract class ProfileUpdater extends Thread {

        private Wrapper wrapper;

        ProfileUpdater(Wrapper wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public void run() {
            ImageIcon newicon = null;
            Image newimage = null;
            Image oldimage = null;
            int count = 0;
            while (wrapper.todownload) {
                try {
                    Thread.sleep(1000);
                    System.out.println("download attempt " + (++count));
                    URL url = new URL(res.getString("downloadfile"));
                    newimage = javax.imageio.ImageIO.read(url);
                    wrapper.todownload = false;
                    wrapper.downloaded = true;
                    oldimage = wrapper.currimage;
                    onDownloadComplete(newimage);
                } catch (Exception ex) {
                    onDownloadFail();
                }
            }
            if (wrapper.downloaded) {
                File oldfile = new File("afzalex_old.png");
                while (wrapper.downloaded && !wrapper.updated) {
                    try {
                        Thread.sleep(1000);
                        if (oldimage != null) {
                            javax.imageio.ImageIO.write((RenderedImage) oldimage, "png", oldfile);
                        }
                        File imagefile = new File("afzalex.png");
                        javax.imageio.ImageIO.write((RenderedImage) newimage, "png", imagefile);
                        wrapper.updated = true;
                        onUpdateComplete();
                    } catch (InterruptedException | IOException ex) {
                        System.out.println("Error while saving new file: " + ex);
                        onUpdateFail();
                    }
                }
            } else {
                System.out.println("Failed to download file");
            }
        }

        public abstract void onDownloadComplete(Image newimage);

        public abstract void onDownloadFail();

        public abstract void onUpdateComplete();

        public abstract void onUpdateFail();

    }

    public void setTray() {
        PopupMenu popupmenu = new PopupMenu("Stack Overflow Profile");
        MenuItem update = new MenuItem("Update");
        MenuItem show = new MenuItem("Show Profile");
        MenuItem showInBrowser = new MenuItem("Show in Browser");
        MenuItem close = new MenuItem("Close");
        update.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateProfile(wrapper);
            }
        });
        showInBrowser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInBrowser();
            }
        });
        show.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showProfile(wrapper, PROFILE_VIEW_TIME);
            }
        });
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        popupmenu.add(show);
        popupmenu.add(update);
        popupmenu.addSeparator();
        popupmenu.add(showInBrowser);
        popupmenu.addSeparator();
        popupmenu.add(close);
        TrayIcon trayicon = new TrayIcon(iconimage.getScaledInstance(18, 18, Image.SCALE_SMOOTH), "Stack Overflow Profile");
        trayicon.setImageAutoSize(true);
        trayicon.setPopupMenu(popupmenu);
        trayicon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showProfile(wrapper, PROFILE_VIEW_TIME);
            }
        });
        try {
            SystemTray.getSystemTray().add(trayicon);
        } catch (AWTException ex) {
            System.out.println("Cannot add icon in System Tray");
        }
    }

    class Wrapper {

        Image currimage = null;
        transient boolean ishidden = true;
        transient boolean todownload = true;
        transient boolean downloaded = false;
        transient boolean updated = false;
        transient boolean isclosed = false;
        transient boolean mousein = false;

        public void resetToDownload() {
            todownload = true;
            downloaded = false;
            updated = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StackOverflowProfile sop = new StackOverflowProfile();
            }
        });
    }
}
