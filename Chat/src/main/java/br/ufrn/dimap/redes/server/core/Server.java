package br.ufrn.dimap.redes.server.core;

import br.ufrn.dimap.redes.server.handler.ClientHandler;


import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {

    //  Selciona porta pouco requisitada
    public static final int PORT = 12345;

    /* Estrutura que armazena todos os clientes conectados;
       Obs: Hash map para ser thread safe e permitir melhor acesso concorrente
       Obs2: Valor / ClientHander -> vai ler e passar as mensagens pros clientes
       Chave = String que representa o nome de cada usuário */
    private static final ConcurrentHashMap<String,ClientHandler> clients = new ConcurrentHashMap<>();

    // Função main
    public static void main(String[] args) {
        System.out.println("Servidor de Chat iniciado na porta " + PORT);

        // Inicialização do servidor
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) { // Mantém o loop para cadastro de novos clientes
                Socket sock = serverSocket.accept(); // Bloqueia até um cliente entrar e devolve um Socket
                new Thread(new ClientHandler(sock)).start(); // Cria um ClientHandler pro novo cliente
            }
        } catch (IOException e) { // Catch de erro ao criar ou aceitar socket
            e.printStackTrace();
        }
    }

    // Método de registro de cliente no Hashmap
    public static boolean registerClient(String nick, ClientHandler h) { // recebe nome e ClientHandler
        if (clients.putIfAbsent(nick,h)==null) { // testa se o nome ou ClientHandler já existem no hashmap
            System.out.println("DEBUG: " + nick + " registrado. Total de clientes = " + clients.size());
            broadcast("[" + nick + " entrou]", h); // Avisa em broadcast aos outros clientes que o novo entrou
            return true;
        }
        h.send("Nome" + nick + " já em uso. Tente outro!");
        return false; // Não registra se já existir outro
    }

    // Método para remover cliente
    public static void removeClient(String nick) { // Recebe apenas o nome
        clients.remove(nick);
        broadcast("[" + nick + " saiu]", null); // Mensagem de Broadcast para avisar que um cliente saiu aos outros
    }

    // Método para envio de mensagens a todos (ou quase todos)
    public static void broadcast(String msg, ClientHandler exclude) {
        for (ClientHandler ch : clients.values()) { // Passa por todos os clientes registrados
            if (ch!=exclude) ch.send(msg); // Se não for um cliente excluido, envia a mensagem
        }
    }

    // Método que envia mensagem privada para um usuário em específico
    public static boolean sendPrivate(String toNick, String msg, ClientHandler from) { // from: ClientHandler de quem envia
        ClientHandler target = clients.get(toNick); // Busca handler do cliente em especifico
        if (target!=null) { // Checa se o resultado veio nulo
            target.send("[privado de " + from.getNick() + "]: " + msg); // Se não veio nulo, formata e envia a mensagem
            return true;
        }
        from.send("Usuário " + toNick + " não encontrado.");
        return false; // Caso não encontre, avisa ao cliente e não manda mensagem
    }
}

