//package org.example;
//import org.telegram.telegrambots.bots.TelegramLongPollingBot;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class Tryout extends TelegramLongPollingBot {
//    Map<Long,String> userState = new HashMap<>();
//    List<Long> subscribers =new ArrayList<>();
//    int qNum;
//    int aNum;
//    @Override
//    public String getBotUsername() {
//        return null;
//    }
//
//    @Override
//    public String getBotToken() {
//        return null;
//    }
//
//    @Override
//    public void onUpdateReceived(Update update) {
//        System.out.println("got it");
//        Long userId= update.getMessage().getChatId();
//        String text = update.getMessage().getText();
//
//        String state = userState.getOrDefault(userId,"START");
//
//
//        switch (state){
//            case "START":
//                start(userId,text);
//                break;
//            case "WAITING_FOR_ANSWER":
//                waitingForAnswer(userId,text);
//                break;
//            case "WAITING_FOR_Q_NUM":
//                waitingForQNum(userId,text);
//                break;
//            case "WAITING_FOR_A_NUM":
//                waitingForANum(userId,text);
//                break;
//        }
//    }
//
//    public void start(Long userId,String text){
//        System.out.println("Start");
//
//        sendMessage(userId,"Menu:\n '/sub' to subscribe\n '/survey' to send a survey");
//        userState.put(userId,"WAITING_FOR_ANSWER");
//
//        if (text.equalsIgnoreCase("/survey")){
//            sendMessage(userId,"Enter '/addQuestion' to add a question");
//            userState.put(userId,"");
//        }
//    }
//    public void waitingForAnswer(Long userId,String text){
//        switch (text) {
//            case "/sub":
//                if(subscribers.contains(userId)){
//                    sendMessage(userId,"You're already subscribed");
//                }else{
//                    subscribers.add(userId);
//                    sendMessage(userId,"You have successfully subscribed");
//                }
//            case "/survey":
//                if(subscribers.contains(userId)){
//                    sendMessage(userId,"How many questions(1-3) would you like to send?");
//                    userState.put(userId,"WAITING_FOR_Q_NUM");
//                }else{
//                    sendMessage(userId,"subscribe first, enter '/sub' to subscribe");
//                }
//        }
//
//    }
//    public void survey (Long userId,String text){
//
//    }
//    public void waitingForQNum(Long userId,String text){
//        switch (text){
//            case "1":
//                qNum = 1;
//                sendMessage(userId,"Great! and how many answers? (2-4)");
//                userState.put(userId,"WAITING_FOR_A_NUM");
//                break;
//            case "2":
//                qNum = 2;
//                sendMessage(userId,"Great! and how many answers? (2-4)");
//                userState.put(userId,"WAITING_FOR_A_NUM");
//                break;
//            case "3":
//                qNum = 3;
//                sendMessage(userId,"Great! and how many answers? (2-4)");
//                userState.put(userId,"WAITING_FOR_A_NUM");
//                break;
//            default:
//                sendMessage(userId,"Sorry, That's not an option");
//                break;
//        }
//    }
//    public void waitingForANum(Long userId, String text) {
//        switch (text){
//            case "2":
//                aNum = 2;
//                userState.put(userId,"WAITING_FOR_SURVEY");
//                break;
//            case "3":
//                aNum =3;
//                userState.put(userId,"WAITING_FOR_SURVEY");
//                break;
//            case "4":
//                aNum =4;
//                userState.put(userId,"WAITING_FOR_SURVEY");
//                break;
//            default:
//                sendMessage(userId,"Sorry that's not an option");
//                break;
//        }
//    }
//    public void sendMessage(Long userId,String text){
//        SendMessage sendMessage = new SendMessage();
//        sendMessage.setChatId(String.valueOf(userId));
//        sendMessage.setText(text);
//        try {
//            execute(sendMessage);
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
