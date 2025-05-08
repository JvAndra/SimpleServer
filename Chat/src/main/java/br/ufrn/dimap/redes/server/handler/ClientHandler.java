package br.ufrn.dimap.redes.server.handler;

import br.ufrn.dimap.redes.server.core.Server;

import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Classe que vai tratar o cliente
public class ClientHandler implements Runnable {
    private Socket socket;               // Socket da conexão com o cliente
    private BufferedReader in;           // Leitor de texto (linhas), BufferedReader permite maior eficiência (menos chamadas de sistema)
    private PrintWriter out;             // Escritor de texto para o cliente, PrinterWriter evita ficar chamando out.flush
    private String nick;                 // Nome escolhido pelo cliente

    public ClientHandler(Socket socket) { // Construtor / recebe socket
        this.socket = socket;
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8")); // Prepara leitor de linhas do cliente
            out = new PrintWriter(socket.getOutputStream(), true); // Prepara escritor de linhas do cliente
        } catch (IOException e) { // Catch caso haja algum problema na criação do leitor e escritor
            e.printStackTrace();
        }
    }

    // Método que retorna o nome do cliente
    public String getNick() { 
        return nick; 
    }

    // Método que envia uma mensagem para o cliente
    public void send(String msg) { 
        out.println(msg); 
    }

    @Override
    public void run() { // Atendimento ao cliente
        try {

            while (true) { // Loop de login
                out.println("Digite seu nick:");
                String line = in.readLine();

                if (line==null) { // Cliente desconectou antes que pudesse escrever algo
                    socket.close();
                    return;
                }

                String desired = line.trim(); // Remoção de espaços em branco
                if (desired.isEmpty()) { // Checa se  é vazio
                    out.println("Nick não pode ser vazio.");
                    continue;
                }

                if (Server.registerClient(desired, this)) { // Tenta registrar no servidor
                    nick = desired;
                    break; // Cadastra e sai do loop
                } else {
                    out.println("Nick '" + desired + "' já em uso. Tente outro."); // Avisa o cliente e continua o loop
                }
            }

            // Mensagens iniciais para o cliente
            out.println("Bem-vindo, " + nick + "!");
            out.println("Para privado: @\"nick completo\" mensagem");
            out.println("Para geral: digite a mensagem normalmente");
            out.println("Para sair: /exit");

            // Recebimento de mensagens
            String msg;

            while ((msg = in.readLine()) != null) { // Continua lendo até receber null (cliente se desconectar)

                if (msg.equalsIgnoreCase("/exit")) break;

                if (msg.startsWith("@")) {
                    // Ativador de mensagem privada @ "nome composto" mensagem
                    Pattern p = Pattern.compile("^@\"([^\"]+)\"\\s+(.+)$"); // Tira todo espaço e caracteres descnecessários e guarda nome e mensagem
                    Matcher m = p.matcher(msg);
                    if (m.matches()) {
                        String target = m.group(1);  // nome
                        String text   = m.group(2);  // mensagem

                        if (!Server.sendPrivate(target, text, this)) { // Tenta enviar, se falhar, retorna mensagem de error
                            out.println("Usuário '" + target + "' não existe."); 
                        }
                    } else {
                        out.println("Formato privado inválido. Use @\"Nick Completo\" mensagem"); // Caso falte espaço entre nome e mensagem, retorna o jeito certo
                    }
                } else { //Caso não seja mensagem privada, faz um broadcast a todos os clientes
                    Server.broadcast("[" + nick + "]: " + msg, this); // This para remover algum remetente caso queira
                }
            }

        } catch (IOException ignored) { // Erros de sistema 
        } finally { 
            // Limpeza e desconexão
            try { socket.close(); // Garante fechamento do socket
            } catch (IOException ignored) {}

            if (nick != null) Server.removeClient(nick); // Remove cliente do mapa do server e notifica a saída
        }
    }
}