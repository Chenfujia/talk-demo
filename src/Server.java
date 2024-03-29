import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static Map<String,ClientHandler> clientMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器启动成功"+serverSocket.getLocalSocketAddress()+"等待客户端连接...");

        //监控客户端连接，每隔1s确认一次，如果客户端没有响应，就断开连接
        executorService.execute(()->{
            while (true){
                try {
                    Thread.sleep(1000);
                    for (Map.Entry<String,ClientHandler> entry:clientMap.entrySet()){
                        if (!entry.getValue().isAlive()){
                            clientMap.remove(entry.getKey());
                            System.out.println("客户端断开连接: " + entry.getKey());
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("与客户端连接断开");
                }
            }
        });

        while (true){
            Socket client= serverSocket.accept();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String userName = bufferedReader.readLine();
            //地址和端口
            System.out.println("用户"+userName+"已连接\tIP:"+client.getInetAddress().getHostAddress()+":"+client.getPort());

            ClientHandler clientHandler = new ClientHandler(client, userName);
            clientMap.put(userName,clientHandler);

            executorService.execute(clientHandler);
        }

    }

    static class ClientHandler implements Runnable {
        public Socket clientSocket;

        public String userName;

        public BufferedReader in;
        public PrintWriter out;

        public ClientHandler(Socket socket,String userName) {
            this.clientSocket = socket;
            this.userName = userName;
            openStream(clientSocket);
        }

        @Override
        public void run() {
            try {
                // 向客户端发送欢迎消息
                out.println(userName+":欢迎连接到服务器! 您的IP:"+clientSocket.getInetAddress().getHostAddress()+"输入exit可以断开连接\n在线用户列表:可输入用户名建立连接:"+clientMap.keySet());
                //广播谁上线了
                for (Map.Entry<String,ClientHandler> entry:clientMap.entrySet()){
                    if (!entry.getKey().equals(userName)){
                        entry.getValue().out.println(userName+"上线了!");
                        entry.getValue().out.println("当前在线用户列表:"+clientMap.keySet());
                    }
                }
                // 循环接收和发送消息，直到客户端关闭连接
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("客户端发来消息: " + message);
                    if ("exit".equals(message)) {
                        out.println("再见!");
                        break;
                    }
                    if (clientMap.containsKey(message)){
                        String toUser = message;
                        System.out.println("客户端发来消息: " + toUser);
                        out.println("已连接到用户:"+toUser+"      输入esc可退出到选人界面");
                        while (true){
                            out.println("请输入消息:");
                            String msg = in.readLine();
                            if ("esc".equals(msg)){
                                out.println("当前在线用户列表:"+clientMap.keySet());
                                break;
                            }
                            if ("exit".equals(msg)){
                                out.println("再见!");
                                return;
                            }
                            clientMap.get(toUser).out.println(userName+":说:"+msg);
                            System.out.println(this.userName+"对"+toUser+"说:"+msg);
                        }
                    }else if("exit".equals(message)){
                        out.println("再见!");
                        return;
                    }else {
                        out.println("用户不存在,请重新输入:");
                    }

                }
            } catch (IOException e) {
                System.out.println("客户已下线: " + userName);
            }finally {
                try {
                    // 客户端断开连接，移除客户端
                    clientMap.remove(userName);
//                   广播谁下线了
                    for (Map.Entry<String,ClientHandler> entry:clientMap.entrySet()){
                        entry.getValue().out.println(userName+"下线了!");
                        entry.getValue().out.println("在线用户列表:"+clientMap.keySet());
                    }
                    out.println("您已下线!");
                    closeStream();
                    clientSocket.close();
                    System.out.println("客户端断开连接: " + userName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private boolean openStream(Socket clientSocket){
            try {
                // 获取客户端输入流，用于接收消息
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // 获取客户端输出流，用于发送消息
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                return true;
            } catch (IOException e) {
                System.err.println("用户开启流失败: " + e.getMessage());
                return false;
            }
        }

        private void closeStream(){
            try {
                if (this.in!=null){
                    this.in.close();
                }
                if (this.out!=null){
                    this.out.close();
                }
            } catch (IOException e) {
                System.out.println("用户关闭流失败: " + e.getMessage());
            }
        }

        private boolean isAlive() {
            return Thread.currentThread().isAlive();
        }
    }
}
