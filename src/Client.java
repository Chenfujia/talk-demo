import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {


    public static void main(String[] args) {


        String serverHost = "localhost"; // 服务器地址
        int serverPort = 8888; // 服务器端口
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            Socket socket = new Socket(serverHost, serverPort);// 连接服务器

            BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("连接到服务器成功，请输入你的用户名：");
            String userName = userInput.readLine();
            serverOutput.println(userName);

            executorService.execute(() -> {
                try {
                    String message;
                    while ((message = serverInput.readLine()) != null) {
                        if (message.equals("您已下线!")) {
                            System.out.println(message);
                            System.exit(0);
                        }
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("服务器已关闭！");
                    System.exit(0);
                }
            });
            String inputMessage;
            while ((inputMessage = userInput.readLine()) != null) {
                serverOutput.println(inputMessage);
            }

            socket.close();
            serverInput.close();
            serverOutput.close();
            userInput.close();

        }catch (IOException e){
            System.out.println("服务器连接失败");
        }

    }

}
