// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)
// Source File Name:   KTSAuth.java

package net.jp.kts.syuuryan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class KTSAuth extends JavaPlugin implements Listener {
    Logger log;

    public void onEnable()
    {
        log = getLogger();
        boolean onlineMode = getServer().getOnlineMode();
        if(onlineMode)
            getServer().broadcastMessage((new StringBuilder()).append(ChatColor.GREEN).append("Online mode now").toString());
        else
            getServer().broadcastMessage((new StringBuilder()).append(ChatColor.RED).append("Offline mode now").toString());
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void onDisable()
    {
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent player)
    {
    	String ipAddress = null;
    	InetAddress ia = player.getAddress();
    	if (ia != null) {
    		ipAddress = ia.getHostAddress();
    	}

        String userName = player.getPlayer().getName();
        boolean authServerEnabled = pingMinecraftDotNet(userName);
        boolean onlineMode = getServer().getOnlineMode();

        // test用
//        authServerEnabled = false;

        // 127.0.0.1
        if(!authServerEnabled && !onlineMode && ipAddress.equals("127.0.0.1")) {
            log.info("Attempting to AdministratorLogin");
        }
        // プライベートログイン
        else if(!authServerEnabled && !onlineMode) {
            log.info((new StringBuilder("Attempting to PrivateLogin Name:")).append(userName).toString());
            execPrivateLogin(player);
        }
        // オフラインモードに切り替え
        else if(!authServerEnabled) {
            log.info("Setting online-mode FALSE");
            turnOnlineMode("false");
            getServer().reload();
            player.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_OTHER, "\u30B5\u30FC\u30D0\u30FC\u306E\u30EA\u30ED\u30FC\u30C9\u4E2D\u3067\u3059\u3002\u304A\u624B\u6570\u3067\u3059\u304C\u518D\u5EA6\u30ED\u30B0\u30A4\u30F3\u3057\u3066\u304F\u3060\u3055\u3044\u3002");
        }
        // パブリックログイン
        else if(authServerEnabled && onlineMode) {
            log.info((new StringBuilder("Attempting to PublicLogin Name:")).append(userName).toString());
            // 最新のIPアドレスを保存
            try {
            	File authDir = new File("./plugins/KTSAuth");
            	if (authDir.mkdir()) log.info("new directory created in plugins");
            	File authFile = new File("./plugins/KTSAuth/" + userName + ".conf");
            	if (authFile.createNewFile()) log.info("new file created in plugins/KTSAuth");
            	if (!authFile.exists()) log.warning(authFile.getPath() + "does not exists");
            	PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(authFile)));
            	pw.write(ipAddress);
            	pw.close();

            }
            catch (SecurityException e) {
            	log.warning("ディレクトリまたはファイルの作成に失敗しました\n" + e.getMessage());
            	e.printStackTrace();
            }
            catch (IOException e) {
            	log.warning("出力ストリームのオープンに失敗しました\n" + e.getMessage());
				e.printStackTrace();
			}
        }
        // オンラインモードに切り替え
        else {
            log.info("Setting online-mode True");
            turnOnlineMode("true");
            getServer().reload();
            player.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_OTHER, "\u30B5\u30FC\u30D0\u30FC\u306E\u30EA\u30ED\u30FC\u30C9\u4E2D\u3067\u3059\u3002\u304A\u624B\u6570\u3067\u3059\u304C\u518D\u5EA6\u30ED\u30B0\u30A4\u30F3\u3057\u3066\u304F\u3060\u3055\u3044\u3002");
        }
    }

    private void execPrivateLogin(PlayerLoginEvent player)
    {
    	String kickMessage = null;
    	String userName = player.getPlayer().getName();
    	String ipAddress = null;
    	InetAddress ia = player.getAddress();
    	if (ia != null) {
    		ipAddress = ia.getHostAddress();
    	}

    	if (ipAddress == null) {
    		kickMessage = "認証サーバーが復旧するまでログインできません";
    		log.warning(userName + "のIPアドレスを取得できませんでした");
    	}
    	else {
    		try {
    			File authFile = new File("./plugins/KTSAuth/" + userName + ".conf");
    			if (!authFile.exists()) {
    				log.warning("confファイルが存在しません");
    	    		kickMessage = "認証サーバーが復旧するまでログインできません";
    			}
    			else if (!authFile.canRead()) {
    				log.warning("confファイルの読み込みに失敗しました");
    	    		kickMessage = "confファイルの読み込みに失敗しました";
    			}
    			else {
	    			BufferedReader br = new BufferedReader(new FileReader(authFile));
	    			String buf = br.readLine();
	    			log.info("IPアドレスの照会に成功しました");
	    			if (buf.equals(ipAddress)) {
	    				log.info("プレイヤーのIPアドレスは正常です");
	    			}
	    			else {
	    				log.warning("前回のIPアドレスと一致しませんでした");
	    	    		kickMessage = "認証サーバーが復旧するまでログインできません";
	    			}
	    			br.close();
    			}
    		}
            catch (IOException e) {
            	log.warning("入力ストリームのオープンに失敗しました\n" + e.getMessage());
				e.printStackTrace();
			}
    	}

    	if (kickMessage != null) {
    		player.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_OTHER, kickMessage);
    	}
    }

    private void turnOnlineMode(String onlineMode)
    {
        List<String> lProperties;
        File serverProperties;
        lProperties = new ArrayList<String>();
        serverProperties = new File("./server.properties");
        if(serverProperties.exists() && serverProperties.isFile() && serverProperties.canRead()) {
	        boolean updated;
	        try {
		        BufferedReader fr = new BufferedReader(new FileReader(serverProperties));
		        for(String buf = fr.readLine(); buf != null; buf = fr.readLine())
		            lProperties.add(buf);

		        fr.close();
		        updated = false;
		        for(int i = 0; i < lProperties.size(); i++)
		            if(((String)lProperties.get(i)).startsWith("online-mode="))
		            {
		                lProperties.set(i, (new StringBuilder("online-mode=")).append(onlineMode).toString());
		                updated = true;
		            }

		        if(!serverProperties.canWrite())
		        {
		            log.info("Couldn't Write server.properties");
		            return;
		        }
		            if(updated)
		            {
		                BufferedWriter fw = new BufferedWriter(new FileWriter(serverProperties));
		                for (String property : lProperties) {
		                	fw.write(property + "\n");
		                }

		                fw.close();
		            } else
		            {
		                log.info("online-mode doesn't updated");
		            }
		        }
		        catch(FileNotFoundException e)
		        {
		            log.info((new StringBuilder("Couldn't find Name:")).append(serverProperties.getName()).toString());
		        }
		        catch(IOException e)
		        {
		            log.info((new StringBuilder("Couldn't open Name:")).append(serverProperties.getName()).append(" Reason:").append(e.getMessage()).toString());
		        }
        }
        else if(!serverProperties.exists())
            log.info((new StringBuilder("Not exists Name:")).append(serverProperties.getName()).toString());
        else
        if(!serverProperties.isFile())
            log.info((new StringBuilder("Is not file Name:")).append(serverProperties.getName()).toString());
        else
        if(!serverProperties.canRead())
            log.info((new StringBuilder("Not readable Name:")).append(serverProperties.getName()).toString());
    }

    public void onServerReload(ServerCommandEvent command)
    {
        if(command.getCommand().toLowerCase().equals("reload"))
            log.info("Execute command /reload");
    }

    private boolean pingMinecraftDotNet(String userName)
    {
        boolean hasPaid = false;
        String urlString = (new StringBuilder("https://minecraft.net/haspaid.jsp?user=")).append(userName).toString();
        try
        {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(1500);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String buf = in.readLine();
            hasPaid = Boolean.valueOf(buf).booleanValue();
            in.close();
        }
        catch(UnknownHostException e)
        {
            log.info((new StringBuilder("Couldn't reach server Name:")).append(urlString).toString());
        }
        catch(MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return hasPaid;
    }

}
