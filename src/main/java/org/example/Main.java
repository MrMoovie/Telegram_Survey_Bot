package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.*;


public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new TelegramBot());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
class TelegramBot extends TelegramLongPollingBot{
    List<Long> subscribers = new ArrayList<>();
    Map<Long, String> userState = new HashMap<>();
    Map<String,Integer> answers = new HashMap<>();

    List<Long> voters = new ArrayList<>();

    Map<Integer,Survey> surveys = new HashMap<>();
    int time;
    boolean isAvailable =true;
    List<String> result = new ArrayList<>();
    Thread timer = new Thread(()->{
        while (time < 300){
            time++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        calcResults();
        sendMessageToSubscribers("Time limit has been reached, the results are:");
        sendMessageToSubscribers(result.toString());
        isAvailable = true;
        time = 0;
        surveys.clear();
    });

    @Override
    public String getBotUsername() {
        return "@SPBVelvel_bot";
    }

    @Override
    public String getBotToken() {
        return "7171382432:AAHB94Bus5zKeGEmscNCAafayGv1q9yEl8s";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()){
            System.out.println("Has Callback");
            surveyResultsUpdate(update);
            System.out.println(answers.get(update.getCallbackQuery().getData()));
        }
        System.out.println("Got it");


        Long userId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        String state = userState.getOrDefault(userId,"START");//user-admin 1 state

        switch (state){
            case "START":
                start(userId);
                break;
            case "WAITING_FOR_SUB":
                if(text.equalsIgnoreCase("/sub"))
                    waitingForSub(userId,update.getMessage().getFrom().getFirstName()+update.getMessage().getFrom().getLastName());
                else
                    sendMessage(userId,"That's not an option \nEnter '/sub' to subscribe");
                break;
            case "WAITING_FOR_POLL":
                if (isAvailable) {
                    waitingForPoll(userId,update.getMessage());
                } else {
                    sendMessage(userId,"Sorry, the survey right is unavailable");
                }
                break;
            case "WAITING_FOR_ANSWER":
                waitingForAnswer(userId,text);
                break;
            default:
                userState.put(userId,"START");
                break;
        }
    }
    public void surveyResultsUpdate(Update response){
        String callBackQuery = response.getCallbackQuery().getData().split("_")[0];
        int id = Integer.parseInt(response.getCallbackQuery().getData().split("_")[1]);
        int current_answer_count =surveys.get(id).getAnswers().get(callBackQuery);
        Long userId = response.getCallbackQuery().getFrom().getId();
        Survey current_survey = surveys.get(id);

        if(surveys.containsKey(id)){
            if (time<300) {
                if(!current_survey.getVoters().containsValue(userId))
                {
                    current_survey.addVoters(id,userId);
                    voters.add(userId);
                    current_survey.setAnswers(callBackQuery,current_answer_count+1);
                    sendMessage(response.getCallbackQuery().getFrom().getId(),"Your vote has been entered");
                }
                else {
                    sendMessage(response.getCallbackQuery().getFrom().getId(),"You already voted");
                }
            } else {
                sendMessage(response.getCallbackQuery().getFrom().getId(),"Survey time limit has been reached");
            }
        }else
            sendMessage(response.getCallbackQuery().getFrom().getId(),"Survey expired");
    }

    public void calcResults(){
        for(Map.Entry<Integer,Survey> entry:surveys.entrySet()){
            result.add(entry.getValue().calc(voters.size()));
        }

    }
    public void start(Long userId){
        if(!subscribers.contains(userId)){
            sendMessage(userId,"Menu:\n'/sub' to subscribe");
            userState.put(userId,"WAITING_FOR_SUB");
        }else{
            sendMessage(userId,"Welcome! this is a bot designed to send surveys, you have 1-3 questions with 2-4 answers each\nMenu:\n'/survey' to send a survey");
            userState.put(userId,"START");
        }
    }
    public void waitingForAnswer(Long userId, String text){
        switch (text){
            case "/sub":
                waitingForSub(userId,text);
                break;
            case "/survey":
                sendMessage(userId,"Enter the first question (Enter it as a poll, with 2-4 options)");
                userState.put(userId,"WAITING_FOR_POLL");
                break;
            default:
                sendMessage(userId,"That's not an option \nEnter '/survey' to subscribe");
                break;
        }
    }
    public void waitingForSub(Long userId,String username){
        if(subscribers.contains(userId)){
            sendMessage(userId,"You're already subscribed");

        }else{
            subscribers.add(userId);
            sendMessage(userId,"You have successfully subscribed");
            sendMessageToSubscribers("New subscriber: "+username);
            sendMessage(userId,"Welcome! this is a bot designed to send surveys, you have 1-3 questions with 2-4 answers each\nMenu:\n'/survey' to send a survey");
            userState.put(userId,"WAITING_FOR_ANSWER");
        }
    }
    public void waitingForPoll(Long userId,Message response){
        if (response.hasPoll()){
            String question = response.getPoll().getQuestion();
            List<String> options = response.getPoll().getOptions().stream().map(PollOption::getText).toList();

            if (options.size()<=4) {
                surveys.put(surveys.size()+1,new Survey(question,options,String.valueOf(surveys.size()+1)));

                sendMessage(userId,"Poll has been successfully added");
                if(surveys.size()==3) {
                    sendMessage(userId, "Great! You have reached max questions");
                    sendSurveyToSubscribers(userId);
                    timer.start();
                    sendMessage(userId,"Surveys has been successfully sent");
                    userState.put(userId,"WAITING_FOR_RESULTS");
                    isAvailable =false;
                }
                else
                    sendMessage(userId,"Menu:\nEnter another poll (2-4 options) if you'd like to add a question\n '/done' to finish and send the surveys");
            } else {
                sendMessage(userId,"ERROR! too much options");
            }
        }else{
            if(response.getText().equalsIgnoreCase("/done")) {
                sendMessage(userId, "Great!");
                sendSurveyToSubscribers(userId);
                timer.start();
                sendMessage(userId,"Surveys has been successfully sent");
                userState.put(userId,"WAITING_FOR_RESULTS");
                isAvailable =false;
            }else
                sendMessage(userId,"Error,Try again");
        }

    }
    public void sendSurveyToSubscribers(Long exception){
        for(Long id:subscribers){
            System.out.println("Is Subscribed");

            if (!Objects.equals(id, exception)) {
                for (Map.Entry<Integer, Survey> entry : surveys.entrySet()) {
                    entry.getValue().executeSurvey(id);
                }
                sendMessage(id,"You have 5 minutes to answer the survey");
            }

        }
    }
    public void sendMessageToSubscribers(String text){
        for(Long id :subscribers){
            System.out.println(id);
            sendMessage(id,text);
        }
    }
    public void sendMessage(Long chatId, String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }//sends a message

}
class Survey extends TelegramBot{
    private String id;
    private String question;
    private Map<String,Integer> answers= new HashMap<>();
    private SendMessage message =new SendMessage();
    private Map<String,Double> results = new HashMap<>();

    public Map<Integer, Long> getVoters() {
        return voters;
    }

    public void addVoters(int id, Long userId) {
        this.voters.put(id,userId);
    }

    private Map<Integer,Long> voters = new HashMap<>();

    public Map<String, Integer> getAnswers() {
        return answers;
    }
    public void setAnswers(String answer,int count) {
        answers.put(answer,count);
    }
    public String getQuestion(){
        return this.question;
    }
    public String getId(){
        return this.id;
    }
    public Survey(String question, List<String> options, String id){
        this.id = id;
        this.question=question;
        System.out.println("Survey:"+id);

        message.setText(question);


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();

        for(String option:options){
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(option);
            button.setCallbackData(option+"_"+id);

            rowInline1.add(button);
            this.answers.put(option,0);
        }
        rowsInline.add(rowInline1);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);
    }
    public void executeSurvey(Long userId){
        message.setChatId(String.valueOf(userId));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    public String calc(int voters){
        for(Map.Entry<String,Integer> entry:this.answers.entrySet()){
            this.results.put(entry.getKey(),  (entry.getValue()/(double)voters)*100);
        }
        return results.toString();
    }

}