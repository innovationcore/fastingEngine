package fasting.Protocols.HPM_DailyMessage;
//%% NEW FILE HPM_DailyMessageBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.1.6535.66c005ced modeling language!*/


import java.util.*;

/**
 * UML State diagram for a library loan, represented in Umple
 */
// line 3 "model.ump"
// line 50 "model.ump"
public class HPM_DailyMessageBase
{

    //------------------------
    // MEMBER VARIABLES
    //------------------------

    //HPM_DailyMessageBase Attributes
    private int timeout24Hours;

    //HPM_DailyMessageBase State Machines
    public enum State { initial, waitDay, sendHPM_DailyMessage, endProtocol }
    private State state;

    //Helper Variables
    private TimedEventHandler timeoutwaitDayTosendHPM_DailyMessageHandler;

    //------------------------
    // CONSTRUCTOR
    //------------------------

    public HPM_DailyMessageBase()
    {
        timeout24Hours = 0;
        setState(State.initial);
    }

    //------------------------
    // INTERFACE
    //------------------------

    public boolean setTimeout24Hours(int aTimeout24Hours)
    {
        boolean wasSet = false;
        timeout24Hours = aTimeout24Hours;
        wasSet = true;
        return wasSet;
    }

    public int getTimeout24Hours()
    {
        return timeout24Hours;
    }

    public String getStateFullName()
    {
        String answer = state.toString();
        return answer;
    }

    public State getState()
    {
        return state;
    }

    public boolean receivedWaitDay()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case initial:
                setState(State.waitDay);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean timeoutwaitDayTosendHPM_DailyMessage()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case waitDay:
                exitState();
                setState(State.sendHPM_DailyMessage);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean receivedEndProtocol()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case waitDay:
                exitState();
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    private boolean __autotransition1815__()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case sendHPM_DailyMessage:
                setState(State.waitDay);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    private void exitState()
    {
        switch(state)
        {
            case waitDay:
                stopTimeoutwaitDayTosendHPM_DailyMessageHandler();
                break;
        }
    }

    private void setState(State aState)
    {
        state = aState;

        // entry actions and do activities
        switch(state)
        {
            case initial:
                // line 11 "model.ump"
                stateNotify("initial");
                break;
            case waitDay:
                // line 20 "model.ump"
                stateNotify("waitDay");
                startTimeoutwaitDayTosendHPM_DailyMessageHandler();
                break;
            case sendHPM_DailyMessage:
                // line 30 "model.ump"
                stateNotify("sendHPM_DailyMessage");
                __autotransition1815__();
                break;
            case endProtocol:
                // line 37 "model.ump"
                stateNotify("endProtocol");
                break;
        }
    }

    private void startTimeoutwaitDayTosendHPM_DailyMessageHandler()
    {
        timeoutwaitDayTosendHPM_DailyMessageHandler = new TimedEventHandler(this,"timeoutwaitDayTosendHPM_DailyMessage",timeout24Hours);
    }

    private void stopTimeoutwaitDayTosendHPM_DailyMessageHandler()
    {
        timeoutwaitDayTosendHPM_DailyMessageHandler.stop();
    }

    public static class TimedEventHandler extends TimerTask
    {
        private HPM_DailyMessageBase controller;
        private String timeoutMethodName;
        private double howLongInSeconds;
        private Timer timer;

        public TimedEventHandler(HPM_DailyMessageBase aController, String aTimeoutMethodName, double aHowLongInSeconds)
        {
            controller = aController;
            timeoutMethodName = aTimeoutMethodName;
            howLongInSeconds = aHowLongInSeconds;
            timer = new Timer();
            timer.schedule(this, (long)howLongInSeconds*1000);
        }

        public void stop()
        {
            timer.cancel();
        }

        public void run ()
        {
            if ("timeoutwaitDayTosendHPM_DailyMessage".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutwaitDayTosendHPM_DailyMessage();
                if (shouldRestart)
                {
                    controller.startTimeoutwaitDayTosendHPM_DailyMessageHandler();
                }
                return;
            }
        }
    }

    public void delete()
    {}

    // line 44 "model.ump"
    public boolean stateNotify(String node){
        return true;
    }

    // line 45 "model.ump"
    public int currentTime(){
        return 1;
    }


    public String toString()
    {
        return super.toString() + "["+
                "timeout24Hours" + ":" + getTimeout24Hours()+ "]";
    }
}