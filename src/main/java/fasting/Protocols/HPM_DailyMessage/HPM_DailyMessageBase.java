package fasting.Protocols.HPM_DailyMessage;
//%% NEW FILE HPM_DailyMessageBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.1.6535.66c005ced modeling language!*/


import java.util.*;

/**
 * UML State diagram for a library loan, represented in Umple
 */
// line 3 "model.ump"
public class HPM_DailyMessageBase
{

    //------------------------
    // MEMBER VARIABLES
    //------------------------

    //HPM_DailyMessageBase Attributes
    private int timeout24Hours;

    //HPM_DailyMessageBase State Machines
    public enum State { initial, waitDay, sendDailyMessage, endProtocol }
    private State state;

    //Helper Variables
    private TimedEventHandler timeoutwaitDayTosendDailyMessageHandler;

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

    public boolean receivedEndProtocol()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case initial:
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
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

    public boolean timeoutwaitDayTosendDailyMessage()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case waitDay:
                exitState();
                setState(State.sendDailyMessage);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    private boolean __autotransition11967__()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case sendDailyMessage:
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
                stopTimeoutwaitDayTosendDailyMessageHandler();
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
                // line 9 "model.ump"
                stateNotify("initial");
                break;
            case waitDay:
                // line 20 "model.ump"
                stateNotify("waitDay");
                startTimeoutwaitDayTosendDailyMessageHandler();
                break;
            case sendDailyMessage:
                // line 30 "model.ump"
                // send daily message to participant
                stateNotify("sendDailyMessage");
                __autotransition11967__();
                break;
            case endProtocol:
                // line 39 "model.ump"
                stateNotify("endProtocol");
                break;
        }
    }

    private void startTimeoutwaitDayTosendDailyMessageHandler()
    {
        timeoutwaitDayTosendDailyMessageHandler = new TimedEventHandler(this,"timeoutwaitDayTosendDailyMessage",timeout24Hours);
    }

    private void stopTimeoutwaitDayTosendDailyMessageHandler()
    {
        timeoutwaitDayTosendDailyMessageHandler.stop();
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
            if ("timeoutwaitDayTosendDailyMessage".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutwaitDayTosendDailyMessage();
                if (shouldRestart)
                {
                    controller.startTimeoutwaitDayTosendDailyMessageHandler();
                }
                return;
            }
        }
    }

    public void delete()
    {}

    // line 46 "model.ump"
    public boolean stateNotify(String node){
        return true;
    }

    // line 47 "model.ump"
    public int currentTime(){
        return 1;
    }


    public String toString()
    {
        return super.toString() + "["+
                "timeout24Hours" + ":" + getTimeout24Hours()+ "]";
    }
}