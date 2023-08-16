package fasting.Protocols.HPM_Restricted;
//%% NEW FILE HPM_RestrictedBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.1.6535.66c005ced modeling language!*/


import java.util.*;

/**
 * UML State diagram for a library loan, represented in Umple
 */
// line 3 "model.ump"
// line 179 "model.ump"
public class HPM_RestrictedBase
{

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //HPM_RestrictedBase Attributes
  private int startDeadline;
  private int startWarnDeadline;
  private int endDeadline;
  private int endWarnDeadline;
  private int endOfEpisodeDeadline;

  //HPM_RestrictedBase State Machines
  public enum State { initial, waitStart, dayOffWait, warnStartCal, dayOffWarn, startcal, dayOffStartCal, missedStartCal, warnEndCal, dayOffWarnEndCal, endcal, missedEndCal, endOfEpisode, dayOffEndOfEpisode, resetEpisodeVariables, endProtocol }
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

  public HPM_RestrictedBase()
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
      case endOfEpisode:
        exitState();
        setState(State.dayOffEndOfEpisode);
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

  private boolean __autotransition13048__()
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

  private boolean __autotransition13049__()
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

  private boolean __autotransition13050__()
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

  private boolean __autotransition13051__()
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

  private boolean __autotransition13052__()
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

  private boolean __autotransition13053__()
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

  private boolean __autotransition13054__()
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

  private boolean __autotransition13055__()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case dayOffEndOfEpisode:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition13056__()
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
        // entry point, don't do anything here
        stateNotify("initial");
        break;
      case waitStart:
        // line 30 "model.ump"
        // wait here until a startcal text is received
        stateNotify("waitStart");
        startTimeoutwaitStartTowarnStartCalHandler();
        break;
      case dayOffWait:
        // line 42 "model.ump"
        // receive "dayoff"
        stateNotify("dayOffWait");
        __autotransition13048__();
        break;
      case warnStartCal:
        // line 49 "model.ump"
        // send a reminder message at a certain time
        stateNotify("warnStartCal");
        startTimeoutwarnStartCalTomissedStartCalHandler();
        break;
      case dayOffWarn:
        // line 63 "model.ump"
        stateNotify("dayOffWarn");
        __autotransition13049__();
        break;
      case startcal:
        // line 69 "model.ump"
        // receive additional startcals and update
        // starting time, also receive endcal
        stateNotify("startcal");
        startTimeoutstartcalTowarnEndCalHandler();
        break;
      case dayOffStartCal:
        // line 83 "model.ump"
        stateNotify("dayOffStartCal");
        __autotransition13050__();
        break;
      case missedStartCal:
        // line 89 "model.ump"
        // If user doesn't send startcal during the day
        // then send message and wait for restart
        stateNotify("missedStartCal");
        __autotransition13051__();
        break;
      case warnEndCal:
        // line 97 "model.ump"
        // send warning message about endcal not being
        // received
        stateNotify("warnEndCal");
        startTimeoutwarnEndCalTomissedEndCalHandler();
        break;
      case dayOffWarnEndCal:
        // line 110 "model.ump"
        stateNotify("dayOffWarnEndCal");
        __autotransition13052__();
        break;
      case endcal:
        // line 117 "model.ump"
        // receive additional endcal messages and update
        // end time
        stateNotify("endcal");
        __autotransition13053__();
        break;
      case missedEndCal:
        // line 126 "model.ump"
        // If user doesn't send endcal/stopcal
        // during the day then send message and wait for
        // restart
        stateNotify("missedEndCal");
        __autotransition13054__();
        break;
      case endOfEpisode:
        // line 138 "model.ump"
        stateNotify("endOfEpisode");
        startTimeoutendOfEpisodeToresetEpisodeVariablesHandler();
        break;
      case dayOffEndOfEpisode:
        // line 150 "model.ump"
        stateNotify("dayOffEndOfEpisode");
        __autotransition13055__();
        break;
      case resetEpisodeVariables:
        // line 156 "model.ump"
        // resets dayoff and other variables for the next
        // day
        stateNotify("resetEpisodeVariables");
        __autotransition13056__();
        break;
      case endProtocol:
        // line 166 "model.ump"
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
    private HPM_RestrictedBase controller;
    private String timeoutMethodName;
    private double howLongInSeconds;
    private Timer timer;

    public TimedEventHandler(HPM_RestrictedBase aController, String aTimeoutMethodName, double aHowLongInSeconds)
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

  // line 173 "model.ump"
  public boolean stateNotify(String node){
    return true;
  }

  // line 174 "model.ump"
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