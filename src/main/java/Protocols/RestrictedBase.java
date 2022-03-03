package Protocols;/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.31.1.5860.78bb27cc6 modeling language!*/


import java.util.Timer;
import java.util.TimerTask;

/**
 * UML State diagram for a library loan, represented in Umple
 */
// line 3 "model.ump"
// line 121 "model.ump"
public class RestrictedBase
{

  //------------------------
  // MEMBER VARIABLES
  //------------------------

  //Fasting Attributes
  private int missedEndCals;
  private int deadline;

  //Fasting State Machines
  public enum State { initial, startcal, dayOff, dayOffReceivedStartcal, dayOffReceivedEndCal, endAfterEight, endTooEarly, endTooLate, missedEndCal, twoDaysNoEndCals, reminderMessage, success, endOfEpisode }
  private State state;

  //Helper Variables
  private TimedEventHandler timeoutstartcalTomissedEndCalHandler;
  private TimedEventHandler timeoutdayOffToendOfEpisodeHandler;
  private TimedEventHandler timeoutmissedEndCalTotwoDaysNoEndCalsHandler;
  private TimedEventHandler timeoutmissedEndCalToreminderMessageHandler;

  //------------------------
  // CONSTRUCTOR
  //------------------------

  public RestrictedBase()
  {
    missedEndCals = 0;
    deadline = 1209600000;
    setState(State.initial);
  }

  //------------------------
  // INTERFACE
  //------------------------

  public boolean setMissedEndCals(int aMissedEndCals)
  {
    boolean wasSet = false;
    missedEndCals = aMissedEndCals;
    wasSet = true;
    return wasSet;
  }

  public boolean setDeadline(int aDeadline)
  {
    boolean wasSet = false;
    deadline = aDeadline;
    wasSet = true;
    return wasSet;
  }

  public int getFastExpired()
  {
    return 1000*60*60*24*2;
  }

  public int getMissedEndCals()
  {
    return missedEndCals;
  }

  /**
   * Number of ms to the loan becoming due from entry to current state
   * 2 weeks
   */
  public int getDeadline()
  {
    return deadline;
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
      case startcal:
        exitState();
        // line 26 "model.ump"
        // Send Error about duplicate start
        setState(State.startcal);
        wasEventProcessed = true;
        break;
      case dayOff:
        exitState();
        setState(State.dayOffReceivedStartcal);
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
      case initial:
        setState(State.dayOff);
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
        if (endWithinWindow())
        {
          exitState();
          setState(State.success);
          wasEventProcessed = true;
          break;
        }
        if (endAfterEightPM())
        {
          exitState();
          setState(State.endAfterEight);
          wasEventProcessed = true;
          break;
        }
        if (endTooEarly())
        {
          exitState();
          setState(State.endTooEarly);
          wasEventProcessed = true;
          break;
        }
        if (endTooLate())
        {
          exitState();
          setState(State.endTooLate);
          wasEventProcessed = true;
          break;
        }
        break;
      case dayOffReceivedStartcal:
        setState(State.dayOffReceivedEndCal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutstartcalTomissedEndCal()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case startcal:
        exitState();
        setState(State.missedEndCal);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutdayOffToendOfEpisode()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case dayOff:
        exitState();
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition323__()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case dayOffReceivedEndCal:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition324__()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case endAfterEight:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition325__()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case endTooEarly:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition326__()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case endTooLate:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutmissedEndCalTotwoDaysNoEndCals()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case missedEndCal:
        if (getMissedEndCals()>1)
        {
          exitState();
          setState(State.twoDaysNoEndCals);
          wasEventProcessed = true;
          break;
        }
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  public boolean timeoutmissedEndCalToreminderMessage()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case missedEndCal:
        exitState();
        setState(State.reminderMessage);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition327__()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case twoDaysNoEndCals:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition328__()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case reminderMessage:
        setState(State.endOfEpisode);
        wasEventProcessed = true;
        break;
      default:
        // Other states do respond to this event
    }

    return wasEventProcessed;
  }

  private boolean __autotransition329__()
  {
    boolean wasEventProcessed = false;
    
    State aState = state;
    switch (aState)
    {
      case success:
        setState(State.endOfEpisode);
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
      case startcal:
        stopTimeoutstartcalTomissedEndCalHandler();
        break;
      case dayOff:
        stopTimeoutdayOffToendOfEpisodeHandler();
        break;
      case missedEndCal:
        stopTimeoutmissedEndCalTotwoDaysNoEndCalsHandler();
        stopTimeoutmissedEndCalToreminderMessageHandler();
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
        // line 13 "model.ump"
        // here we need to receive the message to start
        // Possibly send missed fasts messages
        break;
      case startcal:
        startTimeoutstartcalTomissedEndCalHandler();
        break;
      case dayOff:
        // line 38 "model.ump"
        //Send message saying you still want start/end
        startTimeoutdayOffToendOfEpisodeHandler();
        break;
      case dayOffReceivedStartcal:
        // line 46 "model.ump"
        // acknowledge
        break;
      case dayOffReceivedEndCal:
        // line 54 "model.ump"
        // acknowledge
        __autotransition323__();
        break;
      case endAfterEight:
        // line 62 "model.ump"
        // Send random message from set
        __autotransition324__();
        break;
      case endTooEarly:
        // line 68 "model.ump"
        // Send random message from set
        __autotransition325__();
        break;
      case endTooLate:
        // line 74 "model.ump"
        // Send random message from set
        __autotransition326__();
        break;
      case missedEndCal:
        // line 80 "model.ump"
        setMissedEndCals(missedEndCals + 1);
        startTimeoutmissedEndCalTotwoDaysNoEndCalsHandler();
        startTimeoutmissedEndCalToreminderMessageHandler();
        break;
      case twoDaysNoEndCals:
        // line 88 "model.ump"
        // Send Special message
        __autotransition327__();
        break;
      case reminderMessage:
        // line 94 "model.ump"
        // Send message in morning reminding to end episode
        __autotransition328__();
        break;
      case success:
        // line 100 "model.ump"
        // Send Random success message
        __autotransition329__();
        break;
    }
  }

  private void startTimeoutstartcalTomissedEndCalHandler()
  {
    timeoutstartcalTomissedEndCalHandler = new TimedEventHandler(this,"timeoutstartcalTomissedEndCal",deadline);
  }

  private void stopTimeoutstartcalTomissedEndCalHandler()
  {
    timeoutstartcalTomissedEndCalHandler.stop();
  }

  private void startTimeoutdayOffToendOfEpisodeHandler()
  {
    timeoutdayOffToendOfEpisodeHandler = new TimedEventHandler(this,"timeoutdayOffToendOfEpisode",deadline);
  }

  private void stopTimeoutdayOffToendOfEpisodeHandler()
  {
    timeoutdayOffToendOfEpisodeHandler.stop();
  }

  private void startTimeoutmissedEndCalTotwoDaysNoEndCalsHandler()
  {
    timeoutmissedEndCalTotwoDaysNoEndCalsHandler = new TimedEventHandler(this,"timeoutmissedEndCalTotwoDaysNoEndCals",deadline);
  }

  private void stopTimeoutmissedEndCalTotwoDaysNoEndCalsHandler()
  {
    timeoutmissedEndCalTotwoDaysNoEndCalsHandler.stop();
  }

  private void startTimeoutmissedEndCalToreminderMessageHandler()
  {
    timeoutmissedEndCalToreminderMessageHandler = new TimedEventHandler(this,"timeoutmissedEndCalToreminderMessage",deadline);
  }

  private void stopTimeoutmissedEndCalToreminderMessageHandler()
  {
    timeoutmissedEndCalToreminderMessageHandler.stop();
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
      if ("timeoutstartcalTomissedEndCal".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutstartcalTomissedEndCal();
        if (shouldRestart)
        {
          controller.startTimeoutstartcalTomissedEndCalHandler();
        }
        return;
      }
      if ("timeoutdayOffToendOfEpisode".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutdayOffToendOfEpisode();
        if (shouldRestart)
        {
          controller.startTimeoutdayOffToendOfEpisodeHandler();
        }
        return;
      }
      if ("timeoutmissedEndCalTotwoDaysNoEndCals".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutmissedEndCalTotwoDaysNoEndCals();
        if (shouldRestart)
        {
          controller.startTimeoutmissedEndCalTotwoDaysNoEndCalsHandler();
        }
        return;
      }
      if ("timeoutmissedEndCalToreminderMessage".equals(timeoutMethodName))
      {
        boolean shouldRestart = !controller.timeoutmissedEndCalToreminderMessage();
        if (shouldRestart)
        {
          controller.startTimeoutmissedEndCalToreminderMessageHandler();
        }
        return;
      }
    }
  }

  public void delete()
  {}

  // line 111 "model.ump"
  public boolean endWithinWindow(){
    return true;
  }

  // line 112 "model.ump"
  public boolean endAfterEightPM(){
    return true;
  }

  // line 113 "model.ump"
  public boolean endTooEarly(){
    return true;
  }

  // line 114 "model.ump"
  public boolean endTooLate(){
    return true;
  }

  // line 115 "model.ump"
  public int currentTime(){
    return 1;
  }


  public String toString()
  {
    return super.toString() + "["+
            "fastExpired" + ":" + getFastExpired()+ "," +
            "missedEndCals" + ":" + getMissedEndCals()+ "," +
            "deadline" + ":" + getDeadline()+ "]";
  }
}