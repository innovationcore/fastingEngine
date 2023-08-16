package fasting.Protocols.HPM_Control;
//%% NEW FILE HPM_ControlBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.1.6535.66c005ced modeling language!*/


import java.util.*;

/**
 * UML State diagram for HPM_Control, represented in Umple
 */
// line 3 "model.ump"
// line 106 "model.ump"
public class HPM_ControlBase
{

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //HPM_ControlBase Attributes
  private int timeout24Hours;
  private int startWarnDeadline;
  private int endWarnDeadline;

  //HPM_ControlBase State Machines
  public enum State { initial, waitStart, warnStartCal, startcal, warnEndCal, endcal, timeout24, endProtocol }
  private State state;

  //Helper Variables
  private TimedEventHandler timeoutwaitStartTowarnStartCalHandler;
  private TimedEventHandler timeoutwarnStartCalTotimeout24Handler;
  private TimedEventHandler timeoutstartcalTowarnEndCalHandler;
  private TimedEventHandler timeoutwarnEndCalTowaitStartHandler;
  private TimedEventHandler timeoutendcalTowaitStartHandler;

  //------------------------
  // CONSTRUCTOR
  //------------------------

  public HPM_ControlBase()
  {
    timeout24Hours = 0;
    startWarnDeadline = 0;
    endWarnDeadline = 0;
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

  public boolean setStartWarnDeadline(int aStartWarnDeadline)
  {
    boolean wasSet = false;
    startWarnDeadline = aStartWarnDeadline;
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

  public int getTimeout24Hours()
  {
    return timeout24Hours;
  }

  public int getStartWarnDeadline()
  {
    return startWarnDeadline;
  }

  public int getEndWarnDeadline()
  {
    return endWarnDeadline;
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

  public boolean receivedWarnStart()
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
      case endcal:
        exitState();
        setState(State.endProtocol);
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

  public boolean timeoutwarnStartCalTotimeout24()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case warnStartCal:
        exitState();
        setState(State.timeout24);
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
      case endcal:
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

  public boolean timeoutwarnEndCalTowaitStart()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case warnEndCal:
        exitState();
        setState(State.waitStart);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutendcalTowaitStart()
  {
    boolean wasEventProcessed = false;

    State aState = state;
    switch (aState)
    {
      case endcal:
        exitState();
        setState(State.waitStart);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition11905__()
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
        stopTimeoutwaitStartTowarnStartCalHandler();
        break;
      case warnStartCal:
        stopTimeoutwarnStartCalTotimeout24Handler();
        break;
      case startcal:
        stopTimeoutstartcalTowarnEndCalHandler();
        break;
      case warnEndCal:
        stopTimeoutwarnEndCalTowaitStartHandler();
        break;
      case endcal:
        stopTimeoutendcalTowaitStartHandler();
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
        // line 12 "model.ump"
        // entry point, don't do anything here
        stateNotify("initial");
        break;
      case waitStart:
        // line 26 "model.ump"
        // wait here until a startcal text is received
        stateNotify("waitStart");
        startTimeoutwaitStartTowarnStartCalHandler();
        break;
      case warnStartCal:
        // line 37 "model.ump"
        // send a reminder messaage at a certain time
        stateNotify("warnStartCal");
        startTimeoutwarnStartCalTotimeout24Handler();
        break;
      case startcal:
        // line 48 "model.ump"
        // receive additional startcals and update
        // starting time, also receive endcal
        stateNotify("startcal");
        startTimeoutstartcalTowarnEndCalHandler();
        break;
      case warnEndCal:
        // line 63 "model.ump"
        stateNotify("warnEndCal");
        startTimeoutwarnEndCalTowaitStartHandler();
        break;
      case endcal:
        // line 72 "model.ump"
        // receive additional endcal messages and update
        // end time
        stateNotify("endcal");
        startTimeoutendcalTowaitStartHandler();
        break;
      case timeout24:
        // line 83 "model.ump"
        // sends a message if startcal or endcal are not
        // received for the day
        stateNotify("timeout24");
        __autotransition11905__();
        break;
      case endProtocol:
        // line 93 "model.ump"
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

  private void startTimeoutwarnStartCalTotimeout24Handler()
  {
    timeoutwarnStartCalTotimeout24Handler = new TimedEventHandler(this,"timeoutwarnStartCalTotimeout24",timeout24Hours);
  }

  private void stopTimeoutwarnStartCalTotimeout24Handler()
  {
    timeoutwarnStartCalTotimeout24Handler.stop();
  }

  private void startTimeoutstartcalTowarnEndCalHandler()
  {
    timeoutstartcalTowarnEndCalHandler = new TimedEventHandler(this,"timeoutstartcalTowarnEndCal",endWarnDeadline);
  }

  private void stopTimeoutstartcalTowarnEndCalHandler()
  {
    timeoutstartcalTowarnEndCalHandler.stop();
  }

  private void startTimeoutwarnEndCalTowaitStartHandler()
  {
    timeoutwarnEndCalTowaitStartHandler = new TimedEventHandler(this,"timeoutwarnEndCalTowaitStart",timeout24Hours);
  }

  private void stopTimeoutwarnEndCalTowaitStartHandler()
  {
    timeoutwarnEndCalTowaitStartHandler.stop();
  }

  private void startTimeoutendcalTowaitStartHandler()
  {
    timeoutendcalTowaitStartHandler = new TimedEventHandler(this,"timeoutendcalTowaitStart",timeout24Hours);
  }

  private void stopTimeoutendcalTowaitStartHandler()
  {
    timeoutendcalTowaitStartHandler.stop();
  }

  public static class TimedEventHandler extends TimerTask
  {
    private HPM_ControlBase controller;
    private String timeoutMethodName;
    private double howLongInSeconds;
    private Timer timer;

    public TimedEventHandler(HPM_ControlBase aController, String aTimeoutMethodName, double aHowLongInSeconds)
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
      if ("timeoutwarnStartCalTotimeout24".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutwarnStartCalTotimeout24();
        if (shouldRestart)
        {
          controller.startTimeoutwarnStartCalTotimeout24Handler();
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
      if ("timeoutwarnEndCalTowaitStart".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutwarnEndCalTowaitStart();
        if (shouldRestart)
        {
          controller.startTimeoutwarnEndCalTowaitStartHandler();
        }
        return;
      }
      if ("timeoutendcalTowaitStart".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutendcalTowaitStart();
        if (shouldRestart)
        {
          controller.startTimeoutendcalTowaitStartHandler();
        }
        return;
      }
    }
  }

  public void delete()
  {}

  // line 100 "model.ump"
  public boolean stateNotify(String node){
    return true;
  }

  // line 101 "model.ump"
  public int currentTime(){
    return 1;
  }


  public String toString()
  {
    return super.toString() + "["+
            "timeout24Hours" + ":" + getTimeout24Hours()+ "," +
            "startWarnDeadline" + ":" + getStartWarnDeadline()+ "," +
            "endWarnDeadline" + ":" + getEndWarnDeadline()+ "]";
  }
}