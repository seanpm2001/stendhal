/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server;

import marauroa.common.*;
import marauroa.common.net.*;
import marauroa.common.game.*;
import marauroa.server.game.*;

import games.stendhal.common.*;
import games.stendhal.server.entity.*;

import java.util.*;
import java.io.*;

public class StendhalRPRuleProcessor implements IRPRuleProcessor
  {
  private RPServerManager rpman; 
  private RPWorld world;
  
  private List<Player> playersObject;
  private List<Player> playersObjectRmText;
  private List<NPC> npcs;
  
  public StendhalRPRuleProcessor()
    {
    playersObject=new LinkedList<Player>();
    playersObjectRmText=new LinkedList<Player>();
    npcs=new LinkedList<NPC>();
    }


  /**
   * Set the context where the actions are executed.
   *
   * @param rpman
   * @param world
   */
  public void setContext(RPServerManager rpman, RPWorld world)
    {
    try
      {
      this.rpman=rpman;
      this.world=world;
      
      StendhalRPAction.initialize(rpman,world);
      
      for(IRPZone zone: world)
        {
        StendhalRPZone szone=(StendhalRPZone)zone;
        npcs.addAll(szone.getNPCList());
        }
      }
    catch(Exception e)
      {
      Logger.thrown("StendhalRPRuleProcessor::setContext","!",e);
      System.exit(-1);
      }
    }

  public boolean onActionAdd(RPAction action, List<RPAction> actionList)
    {
    Logger.trace("StendhalRPRuleProcessor::onActionAdd",">");
    try
      {
      if(action.get("type").equals("moveto") || action.get("type").equals("move") || action.get("type").equals("face"))
        {
        // Cancel previous moveto actions
        Iterator it=actionList.iterator();
        while(it.hasNext())
          {  
          RPAction act=(RPAction)it.next();
          if(act.get("type").equals("moveto"))
            {
            Logger.trace("StendhalRPRuleProcessor::onActionAdd","D","Removed action: "+act.toString());
            it.remove();
            }
          }
        }
      
      return true;
      }
    catch(AttributeNotFoundException e)
      {
      Logger.thrown("StendhalRPRuleProcessor::onActionAdd","X",e);
      return false;
      }
    finally
      {
      Logger.trace("StendhalRPRuleProcessor::onActionAdd","<");
      }
    }
    
  public boolean onIncompleteActionAdd(RPAction action, List<RPAction> actionList)
    {
    Logger.trace("StendhalRPRuleProcessor::onIncompleteActionAdd",">");
    try
      {
      if(action.get("type").equals("moveto"))
        {
        // Cancel this action because there is a new more important one
        Iterator it=actionList.iterator();
        while(it.hasNext())
          {  
          RPAction act=(RPAction)it.next();
          if(act.get("type").equals("moveto") || act.get("type").equals("move") || act.get("type").equals("face"))
            {
            Logger.trace("StendhalRPRuleProcessor::onActionAdd","D","Not readded action: "+action.toString());
            return false;
            }
          }
        }
      
      return true;
      }
    catch(AttributeNotFoundException e)
      {
      Logger.thrown("StendhalRPRuleProcessor::onIncompleteActionAdd","X",e);
      return false;
      }
    finally
      {
      Logger.trace("StendhalRPRuleProcessor::onIncompleteActionAdd","<");
      }
    }
  

  public RPAction.Status execute(RPObject.ID id, RPAction action)
    {
    Logger.trace("StendhalRPRuleProcessor::execute",">");

    RPAction.Status status=RPAction.Status.SUCCESS;

    try
      {
      Player player=(Player)world.get(id);
      
      if(action.get("type").equals("move"))
        {
        move(player,action);
        }
      else if(action.get("type").equals("moveto"))
        {
        status=moveTo(player,action);
        }
      else if(action.get("type").equals("change"))
        {
        changeZone(player,action);
        }
      else if(action.get("type").equals("chat"))
        {
        chat(player,action);
        }          
      else if(action.get("type").equals("face"))
        {
        face(player,action);
        }          
      }
    catch(Exception e)
      {
      Logger.trace("StendhalRPRuleProcessor::execute","X",action.toString());
      Logger.thrown("StendhalRPRuleProcessor::execute","X",e);
      }
    finally
      {
      Logger.trace("StendhalRPRuleProcessor::execute","<");
      }

    return status;
    }
  
  private RPAction.Status moveTo(Player player, RPAction action) throws AttributeNotFoundException, NoRPZoneException
    {
    RPAction.Status status=RPAction.Status.INCOMPLETE;
    
    double dsx=action.getDouble("x");
    double dsy=action.getDouble("y");
    double x=player.getx();
    double y=player.gety();
    
    if(!action.has("moving") || (player.getdx()==0 && player.getdy()==0))
      {
      action.put("moving",1);
      player.setdx(((dsx-x)/10>1?1:(dsx-x)/10));
      player.setdy(((dsy-y)/10>1?1:(dsy-y)/10));
      }
    
    if(Math.abs(dsx-x)<0.1 || Math.abs(dsy-y)<0.1)
      {
      player.stop();
      status=RPAction.Status.SUCCESS;
      }
      
    world.modify(player);
    return status;
    }
    
  private void move(Player player, RPAction action) throws AttributeNotFoundException, NoRPZoneException
    {
    Logger.trace("StendhalRPRuleProcessor::move",">");
    if(action.has("dx")) 
      { 
      double dx=action.getDouble("dx");
      player.setdx(dx<1?dx:1);
      }
      
    if(action.has("dy")) 
      {
      double dy=action.getDouble("dy");
      player.setdy(dy<1?dy:1);
      }
    
    StendhalRPAction.face(player,player.getdx(),player.getdy());
    world.modify(player);
    
    Logger.trace("StendhalRPRuleProcessor::move","<");
    }
   
  private void changeZone(Player player, RPAction action) throws AttributeNotFoundException, NoRPZoneException, NoEntryPointException
    {
    Logger.trace("StendhalRPRuleProcessor::changeZone",">");
    if(action.has("dest") && !player.get("zoneid").equals(action.get("dest")))
      {
      StendhalRPAction.changeZone(player,action.get("dest"));
      StendhalRPAction.transferContent(player);
      }
    Logger.trace("StendhalRPRuleProcessor::changeZone","<");
    }
  
  private void chat(Player player, RPAction action) throws AttributeNotFoundException, NoRPZoneException
    {
    Logger.trace("StendhalRPRuleProcessor::chat",">");
    if(action.has("text")) 
      {
      player.put("text",action.get("text"));
      world.modify(player);
      
      playersObjectRmText.add(player);
      }
    Logger.trace("StendhalRPRuleProcessor::chat","<");
    }

  private void face(Player player, RPAction action) throws AttributeNotFoundException, NoRPZoneException
    {
    Logger.trace("StendhalRPRuleProcessor::face",">");
    if(action.has("dir")) 
      {
      player.setFacing(action.getInt("dir"));
      player.stop();
      world.modify(player);
      }
    Logger.trace("StendhalRPRuleProcessor::face","<");
    }

  /** Notify it when a new turn happens */
  synchronized public void beginTurn()
    {
    Logger.trace("StendhalRPRuleProcessor::beginTurn",">");
    try
      {
      for(Player object: playersObjectRmText)
        {
        if(object.has("text"))
          {
          object.remove("text");
          world.modify(object);
          }
        }
      
      playersObjectRmText.clear();
      }
    catch(Exception e)
      {
      Logger.thrown("StendhalRPRuleProcessor::beginTurn","X",e);
      }
    finally
      {
      Logger.trace("StendhalRPRuleProcessor::beginTurn","<");
      }
    }
  private Player getNearestPlayerThatHasSpeaken(NPC npc, double range)
    {
    double x=npc.getx();
    double y=npc.gety();
    
    for(Player player: playersObject)
      {
      double px=player.getx();
      double py=player.gety();
      
      if(Math.abs(px-x)<range && Math.abs(py-y)<range && player.has("text"))
        {
        return player;
        }
      }
    
    return null;
    }
      
  synchronized public void endTurn()
    {
    Logger.trace("StendhalRPRuleProcessor::endTurn",">");
    try
      {
      for(Player object: playersObject)
        {
        if(!object.stopped())
          {
          StendhalRPAction.move(object);
          }
        
        if(object.hasLeave())
          {          
          StendhalRPAction.leaveZone(object);
          }
        }
      
      for(NPC npc: npcs)
        {
        npc.move();
        if(!npc.stopped())
          {
          StendhalRPAction.move(npc);
          }
        
        Player speaker=getNearestPlayerThatHasSpeaken(npc,5);
        if(speaker!=null && npc.chat(speaker))
          {
          world.modify(npc);
          }
        }
      }
    catch(Exception e)
      {
      Logger.thrown("StendhalRPRuleProcessor::endTurn","X",e);
      }
    finally
      {
      Logger.trace("StendhalRPRuleProcessor::endTurn","<");
      }
    }

  synchronized public boolean onInit(RPObject object) throws RPObjectInvalidException
    {
    Logger.trace("StendhalRPRuleProcessor::onInit",">");
    try
      {
      object.put("zoneid","city");
      Player player=new Player(object);
      player.setdx(0);
      player.setdy(0);
      
      world.add(player); 
      StendhalRPAction.transferContent(player);
      
      StendhalRPZone zone=(StendhalRPZone)world.getRPZone(player.getID());
      zone.placeObjectAtEntryPoint(player);
            
      double x=player.getDouble("x");
      double y=player.getDouble("y");
      
      while(zone.collides(player,x,y))
        {
        x=x+(Math.random()*6-3);
        y=y+(Math.random()*6-3);        
        }
        
      player.setx(x);
      player.sety(y);
      
      playersObject.add(player);
      return true;
      }
    catch(Exception e)
      {
      Logger.thrown("StendhalRPRuleProcessor::onInit","X",e);
      return false;
      }        
    finally
      {
      Logger.trace("StendhalRPRuleProcessor::onInit","<");
      }
    }

  synchronized public boolean onExit(RPObject.ID id)
    {
    Logger.trace("StendhalRPRuleProcessor::onExit",">");
    try
      {
      for(Player object: playersObject)
        {
        if(object.getID().equals(id))
          {
          boolean result=playersObject.remove(object);
          Logger.trace("StendhalRPRuleProcessor::onExit","D","Removed Player was "+result);
          break;
          }
        }
      
      world.remove(id);

      return true;
      }
    catch(Exception e)
      {
      Logger.thrown("StendhalRPRuleProcessor::onExit","X",e);
      return true;
      }
    finally
      {
      Logger.trace("StendhalRPRuleProcessor::onExit","<");
      }
    }

  synchronized public boolean onTimeout(RPObject.ID id)
    {
    Logger.trace("StendhalRPRuleProcessor::onTimeout",">");
    try
      {
      for(Player object: playersObject)
        {
        if(object.getID().equals(id))
          {
          playersObject.remove(object);
          break;
          }
        }

      world.remove(id);
        
      return true;
      }
    catch(Exception e)
      {
      Logger.thrown("StendhalRPRuleProcessor::onTimeout","X",e);
      return true;
      }
    finally
      {
      Logger.trace("StendhalRPRuleProcessor::onTimeout","<");
      }
    }
  }


