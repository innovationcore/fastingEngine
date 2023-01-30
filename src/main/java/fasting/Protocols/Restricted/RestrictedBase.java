package fasting.Protocols.Restricted;
//%% NEW FILE RestrictedBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.1.6535.66c005ced modeling language!*/


import java.util.*;

/**
 * UML State diagram for a library loan, represented in Umple
 */
// line 3 "model.ump"
// line 160 "model.ump"
public class RestrictedBase
{

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //RestrictedBase Attributes
  private int startDeadline;
  private int startWarnDeadline;
  private int endDeadline;
  private int endWarnDeadline;
  private int endOfEpisodeDeadline;

  //RestrictedBase State Machines
  public enum State { initial, waitStart, dayOffWait, warnStartCal, dayOffWarn, startcal, dayOffStartCal, missedStartCal, warnEndCal, dayOffWarnEndCal, endcal, missedEndCal, endOfEpisode, resetEpisodeVariables, endProtocol }
  private State state;

  //Helper Variables
  private TimedEventHandler timeoutwaitStartTowarnStartCalHandler;
  private TimedEventHandler timeoutwarnStartCalTomissedStartCalHandler;
  private TimedEventHandler timeoutstartcalTowarnEndCalHandler;
  private TimedEventHandler timeoutwarnEndCalTomissedEndCalHandler;
  private TimedEventHandler timeoutendOfEpisodeToresetEpisodeVariablesHandler;

  //------------------------
  // CONSTRUCTOR
  //------------------------

  public RestrictedBase()
  {
    startDeadline = 0;
    startWarnDeadline = 0;
    endDeadline = 0;
    endWarnDeadline = 0;
    endOfEpisodeDeadline = 0;
    setState(State.initial);
  }

  //------------------------
  // INTERFACE
  //------------------------

  public boolean setStartDeadline(int aStartDeadline)
  {
    boolean wasSet = false;
    startDeadline = aStartDeadline;
    wasSet = true;
    return wasSet;
  }

  public boolean setStartWarnDeadline(int aStartWarnDeadline)
  {
    boolean wasSet = false;
    startWarnDeadline = aStartWarnDeadline;
    wasSet = true;
    return wasSet;
  }

  public boolean setEndDeadline(int aEndDeadline)
  {
    boolean wasSet = false;
    endDeadline = aEndDeadline;
    wasSet = true;
    return wasSet;
  }

  public boolean setEndWarnDeadline(int aEndWarnDeadline)
  {
    boolean wasSet = false;
    endWarnDeadline = aEndWarnDeadline;
    wasSet = true;
    return wasSet;
  }

  public boolean setEndOfEpisodeDeadline(int aEndOfEpisodeDeadline)
  {
    boolean wasSet = false;
    endOfEpisodeDeadline = aEndOfEpisodeDeadline;
    wasSet = true;
    return wasSet;
  }

  public int getStartDeadline()
  {
    return startDeadline;
  }

  public int getStartWarnDeadline()
  {
    return startWarnDeadline;
  }

  public int getEndDeadline()
  {
    return endDeadline;
  }

  public int getEndWarnDeadline()
  {
    return endWarnDeadline;
  }

  public int getEndOfEpisodeDeadline()
  {
    return endOfEpisodeDeadline;
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

  public boolean receivedWarnStartCal()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case initial:
        setState(State.warnStartCal);
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
      case warnStartCal:
        exitState();
        setState(State.startcal);
        wasEventProcessed = true;
        break;
      case startcal:
        exitState();
        // line 73 "model.ump"
        // Send Error about duplicate start
        setState(State.startcal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean recievedWarnEndCal()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case initial:
        setState(State.warnEndCal);
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
      case waitStart:
        exitState();
        setState(State.endProtocol);
        wasEventProcessed = true;
        break;
      case warnStartCal:
        exitState();
        setState(State.endProtocol);
        wasEventProcessed = true;
        break;
      case startcal:
        exitState();
        setState(State.endProtocol);
        wasEventProcessed = true;
        break;
      case warnEndCal:
        exitState();
        setState(State.endProtocol);
        wasEventProcessed = true;
        break;
      case endOfEpisode:
        exitState();
        setState(State.endProtocol);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean receivedDayOff()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case waitStart:
        exitState();
        setState(State.dayOffWait);
        wasEventProcessed = true;
        break;
      case warnStartCal:
        exitState();
        setState(State.dayOffWarn);
        wasEventProcessed = true;
        break;
      case startcal:
        exitState();
        setState(State.dayOffStartCal);
        wasEventProcessed = true;
        break;
      case warnEndCal:
        exitState();
        setState(State.dayOffWarnEndCal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutwaitStartTowarnStartCal()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case waitStart:
        exitState();
        setState(State.warnStartCal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition1634__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case dayOffWait:
        setState(State.waitStart);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutwarnStartCalTomissedStartCal()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case warnStartCal:
        exitState();
        setState(State.missedStartCal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition1635__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case dayOffWarn:
        setState(State.warnStartCal);
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
      case warnEndCal:
        exitState();
        setState(State.endcal);
        wasEventProcessed = true;
        break;
      case endOfEpisode:
        exitState();
        setState(State.endcal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutstartcalTowarnEndCal()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case startcal:
        exitState();
        setState(State.warnEndCal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition1636__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case dayOffStartCal:
        setState(State.startcal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition1637__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case missedStartCal:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutwarnEndCalTomissedEndCal()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case warnEndCal:
        exitState();
        setState(State.missedEndCal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition1638__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case dayOffWarnEndCal:
        setState(State.warnEndCal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition1639__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case endcal:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition1640__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case missedEndCal:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutendOfEpisodeToresetEpisodeVariables()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case endOfEpisode:
        exitState();
        setState(State.resetEpisodeVariables);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition1641__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case resetEpisodeVariables:
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
        stopTimeoutwaitStartTowarnStartCalHandler();
        break;
      case warnStartCal:
        stopTimeoutwarnStartCalTomissedStartCalHandler();
        break;
      case startcal:
        stopTimeoutstartcalTowarnEndCalHandler();
        break;
      case warnEndCal:
        stopTimeoutwarnEndCalTomissedEndCalHandler();
        break;
      case endOfEpisode:
        stopTimeoutendOfEpisodeToresetEpisodeVariablesHandler();
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
        // line 16 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("initial");
        break;
      case waitStart:
        // line 31 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("waitStart");
        startTimeoutwaitStartTowarnStartCalHandler();
        break;
      case dayOffWait:
        // line 44 "model.ump"
        stateNotify("dayOffWait");
        __autotransition1634__();
        break;
      case warnStartCal:
        // line 50 "model.ump"
        stateNotify("warnStartCal");
        startTimeoutwarnStartCalTomissedStartCalHandler();
        break;
      case dayOffWarn:
        // line 62 "model.ump"
        stateNotify("dayOffWarn");
        __autotransition1635__();
        break;
      case startcal:
        // line 68 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("startcal");
        startTimeoutstartcalTowarnEndCalHandler();
        break;
      case dayOffStartCal:
        // line 83 "model.ump"
        stateNotify("dayOffStartCal");
        __autotransition1636__();
        break;
      case missedStartCal:
        // line 89 "model.ump"
        stateNotify("missedStartCal");
        __autotransition1637__();
        break;
      case warnEndCal:
        // line 95 "model.ump"
        stateNotify("warnEndCal");
        startTimeoutwarnEndCalTomissedEndCalHandler();
        break;
      case dayOffWarnEndCal:
        // line 105 "model.ump"
        stateNotify("dayOffWarnEndCal");
        __autotransition1638__();
        break;
      case endcal:
        // line 112 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        stateNotify("endcal");
        __autotransition1639__();
        break;
      case missedEndCal:
        // line 121 "model.ump"
        stateNotify("missedEndCal");
        __autotransition1640__();
        break;
      case endOfEpisode:
        // line 130 "model.ump"
        stateNotify("endOfEpisode");
        startTimeoutendOfEpisodeToresetEpisodeVariablesHandler();
        break;
      case resetEpisodeVariables:
        // line 140 "model.ump"
        stateNotify("resetEpisodeVariables");
        __autotransition1641__();
        break;
      case endProtocol:
        // line 147 "model.ump"
        stateNotify("endProtocol");
        break;
    }
  }

  private void startTimeoutwaitStartTowarnStartCalHandler()
  {
    timeoutwaitStartTowarnStartCalHandler = new TimedEventHandler(this,"timeoutwaitStartTowarnStartCal",startWarnDeadline);
  }

  private void stopTimeoutwaitStartTowarnStartCalHandler()
  {
    timeoutwaitStartTowarnStartCalHandler.stop();
  }

  private void startTimeoutwarnStartCalTomissedStartCalHandler()
  {
    timeoutwarnStartCalTomissedStartCalHandler = new TimedEventHandler(this,"timeoutwarnStartCalTomissedStartCal",startDeadline);
  }

  private void stopTimeoutwarnStartCalTomissedStartCalHandler()
  {
    timeoutwarnStartCalTomissedStartCalHandler.stop();
  }

  private void startTimeoutstartcalTowarnEndCalHandler()
  {
    timeoutstartcalTowarnEndCalHandler = new TimedEventHandler(this,"timeoutstartcalTowarnEndCal",endWarnDeadline);
  }

  private void stopTimeoutstartcalTowarnEndCalHandler()
  {
    timeoutstartcalTowarnEndCalHandler.stop();
  }

  private void startTimeoutwarnEndCalTomissedEndCalHandler()
  {
    timeoutwarnEndCalTomissedEndCalHandler = new TimedEventHandler(this,"timeoutwarnEndCalTomissedEndCal",endDeadline);
  }

  private void stopTimeoutwarnEndCalTomissedEndCalHandler()
  {
    timeoutwarnEndCalTomissedEndCalHandler.stop();
  }

  private void startTimeoutendOfEpisodeToresetEpisodeVariablesHandler()
  {
    timeoutendOfEpisodeToresetEpisodeVariablesHandler = new TimedEventHandler(this,"timeoutendOfEpisodeToresetEpisodeVariables",endOfEpisodeDeadline);
  }

  private void stopTimeoutendOfEpisodeToresetEpisodeVariablesHandler()
  {
    timeoutendOfEpisodeToresetEpisodeVariablesHandler.stop();
  }

  public static class TimedEventHandler extends TimerTask
  {
    private RestrictedBase controller;
    private String timeoutMethodName;
    private double howLongInSeconds;
    private Timer timer;

    public TimedEventHandler(RestrictedBase aController, String aTimeoutMethodName, double aHowLongInSeconds)
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
      if ("timeoutwaitStartTowarnStartCal".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutwaitStartTowarnStartCal();
        if (shouldRestart)
        {
          controller.startTimeoutwaitStartTowarnStartCalHandler();
        }
        return;
      }
      if ("timeoutwarnStartCalTomissedStartCal".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutwarnStartCalTomissedStartCal();
        if (shouldRestart)
        {
          controller.startTimeoutwarnStartCalTomissedStartCalHandler();
        }
        return;
      }
      if ("timeoutstartcalTowarnEndCal".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutstartcalTowarnEndCal();
        if (shouldRestart)
        {
          controller.startTimeoutstartcalTowarnEndCalHandler();
        }
        return;
      }
      if ("timeoutwarnEndCalTomissedEndCal".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutwarnEndCalTomissedEndCal();
        if (shouldRestart)
        {
          controller.startTimeoutwarnEndCalTomissedEndCalHandler();
        }
        return;
      }
      if ("timeoutendOfEpisodeToresetEpisodeVariables".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutendOfEpisodeToresetEpisodeVariables();
        if (shouldRestart)
        {
          controller.startTimeoutendOfEpisodeToresetEpisodeVariablesHandler();
        }
        return;
      }
    }
  }

  public void delete()
  {}

  // line 154 "model.ump"
  public boolean stateNotify(String node){
    return true;
  }

  // line 155 "model.ump"
  public int currentTime(){
    return 1;
  }


  public String toString()
  {
    return super.toString() + "["+
            "startDeadline" + ":" + getStartDeadline()+ "," +
            "startWarnDeadline" + ":" + getStartWarnDeadline()+ "," +
            "endDeadline" + ":" + getEndDeadline()+ "," +
            "endWarnDeadline" + ":" + getEndWarnDeadline()+ "," +
            "endOfEpisodeDeadline" + ":" + getEndOfEpisodeDeadline()+ "]";
  }
}