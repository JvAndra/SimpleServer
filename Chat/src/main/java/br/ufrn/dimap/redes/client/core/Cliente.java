package br.ufrn.dimap.redes.client.core;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) {

        String host = "localhost";
        int port = 12345;

        // Abre leitores e escritores
        try (
            Socket socket = new Socket(host, port); // Conecta ao servidor TCP

            // Preparação do leitor
            BufferedReader servidorIn = new BufferedReader( 
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
            
            // Preparação do escritor
            PrintWriter servidorOut = new PrintWriter(
                socket.getOutputStream(),  true);

            // Leitor do teclado
            Scanner teclado = new Scanner(System.in, StandardCharsets.UTF_8)
        ) {
            // Thread que lê do servidor e imprime no console
            new Thread(() -> {
                try {
                    String linha;

                    // Enquanto houver linhas para serem lidas o loop continua
                    while ((linha = servidorIn.readLine()) != null) {
                        System.out.println(linha);
                    }
                } catch (IOException e) { // Em caso de conexão encerrada pelo servidor
                    System.err.println("Conexão encerrada pelo servidor.");
                }
            }).start();

            // Loop principal: lê do teclado e envia ao servidor
            while (teclado.hasNextLine()) {
                String msg = teclado.nextLine();
                servidorOut.println(msg); // Envia o que o usuário escreveu para o servidor

                if (msg.equalsIgnoreCase("/exit")) { // Encerra o loop e a conexão
                    break;
                }
            }

        } catch (IOException e) { // Em caso de falha na conexão
            System.err.println("Erro de conexão: " + e.getMessage());
        }
    }
}

