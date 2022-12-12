package fasting.Protocols;
//%% NEW FILE BaselineBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.0.6441.414d09714 modeling language!*/


import java.util.Timer;
import java.util.TimerTask;

/**
 * UML State diagram for a library loan, represented in Umple
 */
// line 3 "model.ump"
// line 77 "model.ump"
public class BaselineBase
{

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //BaselineBase Attributes
  private int timeout24Hours;

  //BaselineBase State Machines
  public enum State { initial, waitStart, startcal, endcal, timeout24, endProtocol }
  private State state;

  //Helper Variables
  private TimedEventHandler timeoutwaitStartTotimeout24Handler;
  private TimedEventHandler timeoutstartcalTotimeout24Handler;

  //------------------------
  // CONSTRUCTOR
  //------------------------

  public BaselineBase()
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

  public boolean receivedWaitStart()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case initial:
        setState(State.waitStart);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean receivedStartCal()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case initial:
        setState(State.startcal);
        wasEventProcessed = true;
        break;
      case waitStart:
        exitState();
        setState(State.startcal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutwaitStartTotimeout24()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case waitStart:
        exitState();
        setState(State.timeout24);
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
      case waitStart:
        exitState();
        setState(State.endProtocol);
        wasEventProcessed = true;
        break;
      case startcal:
        exitState();
        setState(State.endProtocol);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean receivedEndCal()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case startcal:
        exitState();
        setState(State.endcal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutstartcalTotimeout24()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case startcal:
        exitState();
        setState(State.timeout24);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition421__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case endcal:
        setState(State.waitStart);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition422__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case timeout24:
        setState(State.waitStart);
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
      case waitStart:
        stopTimeoutwaitStartTotimeout24Handler();
        break;
      case startcal:
        stopTimeoutstartcalTotimeout24Handler();
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
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("initial");
        break;
      case waitStart:
        // line 23 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("waitStart");
        startTimeoutwaitStartTotimeout24Handler();
        break;
      case startcal:
        // line 36 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("startcal");
        startTimeoutstartcalTotimeout24Handler();
        break;
      case endcal:
        // line 47 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("endcal");
        __autotransition421__();
        break;
      case timeout24:
        // line 55 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("timeout24");
        __autotransition422__();
        break;
      case endProtocol:
        // line 64 "model.ump"
        stateNotify("endProtocol");
        break;
    }
  }

  private void startTimeoutwaitStartTotimeout24Handler()
  {
    timeoutwaitStartTotimeout24Handler = new TimedEventHandler(this,"timeoutwaitStartTotimeout24",timeout24Hours);
  }

  private void stopTimeoutwaitStartTotimeout24Handler()
  {
    timeoutwaitStartTotimeout24Handler.stop();
  }

  private void startTimeoutstartcalTotimeout24Handler()
  {
    timeoutstartcalTotimeout24Handler = new TimedEventHandler(this,"timeoutstartcalTotimeout24",timeout24Hours);
  }

  private void stopTimeoutstartcalTotimeout24Handler()
  {
    timeoutstartcalTotimeout24Handler.stop();
  }

  public static class TimedEventHandler extends TimerTask
  {
    private BaselineBase controller;
    private String timeoutMethodName;
    private double howLongInSeconds;
    private Timer timer;

    public TimedEventHandler(BaselineBase aController, String aTimeoutMethodName, double aHowLongInSeconds)
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
      if ("timeoutwaitStartTotimeout24".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutwaitStartTotimeout24();
        if (shouldRestart)
        {
          controller.startTimeoutwaitStartTotimeout24Handler();
        }
        return;
      }
      if ("timeoutstartcalTotimeout24".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutstartcalTotimeout24();
        if (shouldRestart)
        {
          controller.startTimeoutstartcalTotimeout24Handler();
        }
        return;
      }
    }
  }

  public void delete()
  {}

  // line 71 "model.ump"
  public boolean stateNotify(String node){
    return true;
  }

  // line 72 "model.ump"
  public int currentTime(){
    return 1;
  }


  public String toString()
  {
    return super.toString() + "["+
            "timeout24Hours" + ":" + getTimeout24Hours()+ "]";
  }
}