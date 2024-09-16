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
    int sumVotes;

    Map<Integer,Survey> surveys = new HashMap<>();
    int time =0;
    boolean isAvailable =true;
    List<String> result = new ArrayList<>();
    int suspend;

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
        if ((subscribers.size()-1)*surveys.size()==sumVotes){sendMessageToSubscribers("Everybody have answered, the results are:");}
        else{sendMessageToSubscribers("Time limit has been reached, the results are:");}
        sendMessageToSubscribers(result.toString().replace("}, ","%\n").replace("{"," ").replace("[","").replace("]","").replace("}","%"));
        clear();
    });

    @Override
    public String getBotUsername() {
        return "@PalpatinVelvel_Bot";
    }
    public void clear(){
        subscribers.clear();
        userState.clear();
        answers.clear();
        voters.clear();
        sumVotes=0;
        surveys.clear();
        time =0;
        isAvailable =true;
        result.clear();
        suspend =0;
    }

    @Override
    public String getBotToken() {
        return "7486824932:AAHgjFQ5QOe87oUXzZyFff2P24BznOOq9aE";
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

        String state = userState.getOrDefault(userId,"START");//user 1 state

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
                    sendMessage(userId,"Sorry, the survey right now is unavailable");
                }
                break;
            case "WAITING_FOR_ANSWER":
                waitingForAnswer(userId,text);
                break;
            case "WAITING_FOR_ANSWER2":
                if (isAvailable) {
                    if (text.equalsIgnoreCase("/send")){
                        sendMessage(userId,"Great!");
                        timer.start();
                        sendSurveyToSubscribers(userId);
                        isAvailable =false;
                        sendMessage(userId,"Survey has been successfully sent");
                        userState.put(userId,"START");
                    }
                    if (subscribers.size()-1 != 0) {
                        if (text.equalsIgnoreCase("/suspend")){
                            sendMessage(userId,"For how long - in minutes (enter a digit 1-9)");
                            userState.put(userId,"WAITING_FOR_SUSPENDER");
                        }
                        if(!text.equalsIgnoreCase("/send") && !text.equalsIgnoreCase("/suspend")){
                            sendMessage(userId,"ERROR! Try again");
                        }
                    } else {
                        sendMessage(userId,"Sorry, there aren't any subscribers right now, try again later");
                        userState.put(userId,"START");
                    }
                } else {
                    sendMessage(userId,"Sorry, survey is in progress,wait: "+time/60);
                }
                break;
            case "WAITING_FOR_SUSPENDER":
                if (text.matches("\\d+")){
                    suspend = suspend+ Integer.parseInt(text)*60;
                    sendMessage(userId,"Great the survey will be sent in "+Integer.parseInt(text)+" minutes");
                    Thread suspender = new Thread(()->{
                        while (suspend>0){
                            suspend--;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        timer.start();
                        sendSurveyToSubscribers(userId);
                        isAvailable =false;
                        sendMessage(userId,"Suspend time has ended and the surveys has been sent");
                    });
                    suspender.start();
                }else{
                    sendMessage(userId,"Please enter a number (1-9)");
                }
                break;
            default:
                userState.put(userId,"START");
                break;
        }
    }
    public void surveyResultsUpdate(Update response){
        String callBackQuery = response.getCallbackQuery().getData().split("_")[0];//callBackQuery-> "option1_id"
        int id = Integer.parseInt(response.getCallbackQuery().getData().split("_")[1]);
        int current_answer_count =surveys.get(id).getAnswers().get(callBackQuery);
        Long userId = response.getCallbackQuery().getFrom().getId();
        Survey current_survey = surveys.get(id);//Map{id-> Survey}

        if(surveys.containsKey(id)){
            if (time<300) {
                if(!current_survey.getVoters().containsValue(userId))
                {
                    current_survey.addVoters(id,userId);//to check each question, if he already voted
                    if(!voters.contains(userId)){voters.add(userId);}//to count later the sum of voters
                    sumVotes++;
                    current_survey.setAnswers(callBackQuery,current_answer_count+1);//add another vote to the current option
                    sendMessage(userId,"Your vote has been entered");
                }
                else {
                    sendMessage(response.getCallbackQuery().getFrom().getId(),"You already voted");
                }
            } else {
                sendMessage(response.getCallbackQuery().getFrom().getId(),"Survey time limit has been reached");
            }
        }else
            sendMessage(response.getCallbackQuery().getFrom().getId(),"Survey expired");
        if((subscribers.size()-1)*surveys.size()==sumVotes){
            time = 300;
        }
    }

    public void calcResults(){//iterate through the List<Surveys>
        for(Map.Entry<Integer,Survey> entry:surveys.entrySet()){
            result.add(entry.getValue().calc(voters.size()));
        }
    }
    public void start(Long userId){
        if(!subscribers.contains(userId)){
            if (isAvailable) {
                sendMessage(userId,"Menu:\n'/sub' to subscribe");
                userState.put(userId,"WAITING_FOR_SUB");
            } else {
                sendMessage(userId,"You cannot join the community right now, survey is in progress");
            }
        }else{
            if (isAvailable) {
                sendMessage(userId,"Welcome! this is a bot designed to send surveys, you have 1-3 questions with 2-4 answers each\nMenu:\n'/survey' to send a survey");
                userState.put(userId,"WAITING_FOR_ANSWER");
            } else {
                sendMessage(userId,"'/survey' is unavailable, due to survey in progress");
            }
        }
    }
    public void waitingForAnswer(Long userId, String text){
        if (text.equals("/survey")) {
            sendMessage(userId, "Enter the first question (Enter it as a poll, with 2-4 options)");
            userState.put(userId, "WAITING_FOR_POLL");
        } else {
            sendMessage(userId, "That's not an option \nEnter '/survey' to send a survey");
        }
    }
    public void waitingForSub(Long userId,String username){
        subscribers.add(userId);
        sendMessage(userId,"You have successfully subscribed");
        sendMessageToSubscribers("New subscriber: "+username);
        sendMessageToSubscribers("Now the community has "+subscribers.size()+" members");
        sendMessage(userId,"Welcome "+username+"!!!\nthis is a bot designed to send surveys, you have 1-3 questions with 2-4 answers each\nMenu:\n'/survey' to send a survey");
        userState.put(userId,"WAITING_FOR_ANSWER");
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
                    sendMessage(userId,"Menu:\n'/send' to send the survey immediately\n'/suspend' to send it with delay");
                    userState.put(userId,"WAITING_FOR_ANSWER2");
                }
                else
                    sendMessage(userId,"Menu:\nEnter another poll (2-4 options) if you'd like to add a question\n '/done' to finish and send the surveys");
            } else {
                sendMessage(userId,"ERROR! too much options");
            }
        }else{
            if(response.getText().equalsIgnoreCase("/done")) {
                sendMessage(userId, "Great!");
                sendMessage(userId,"Menu:\n'/send' to send the survey immediately\n'/suspend' to send it with delay");
                userState.put(userId,"WAITING_FOR_ANSWER2");
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
    private Map<String,Integer> answers= new HashMap<>();//Map<"Option_1"->0>
    private SendMessage message =new SendMessage();
    private Map<String,Double> results = new HashMap<>();//Map<"Option_1" -> 0.0%>
    private Map<Integer,Long> voters = new HashMap<>();//Map<id -> userId>

    public Map<Integer, Long> getVoters() {
        return voters;
    }//each time a user votes, I use that to check if he already voted

    public void addVoters(int id, Long userId) {
        this.voters.put(id,userId);
    }//each time a user votes he is being added to the list



    public Map<String, Integer> getAnswers() {
        return answers;
    }//to check the current count of votes, in order to increase it each time a user votes
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

        message.setText(question);


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInlines = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();

        for(String option:options){
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(option);
            button.setCallbackData(option+"_"+id);//the reply String

            rowInline1.add(button);
            this.answers.put(option,0);
        }
        rowsInlines.add(rowInline1);
        inlineKeyboardMarkup.setKeyboard(rowsInlines);
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
        return "The results for '"+question+"' are:\n"+results.toString().replace(",","%\n");
    }
}