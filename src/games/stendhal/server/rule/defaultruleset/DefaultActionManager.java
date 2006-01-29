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
package games.stendhal.server.rule.defaultruleset;

import games.stendhal.server.entity.RPEntity;
import games.stendhal.server.entity.item.Item;
import games.stendhal.server.rule.ActionManager;
import marauroa.common.game.RPSlot;
import marauroa.common.game.RPObject;
import java.util.List;
import java.util.Iterator;

import games.stendhal.server.entity.item.Stackable;

/**
 *
 * @author Matthias Totz
 */
public class DefaultActionManager implements ActionManager
{
  
  /** the singleton instance, lazy initialisation */
  private static DefaultActionManager manager;
  
  /** no public constuctor */
  private DefaultActionManager()
  {
  }

  /** 
   * returns the instance of this manager.
   * Note: This method is synchonized.
   */
  public static synchronized DefaultActionManager getInstance()
  {
    if (manager == null)
    {
      manager = new DefaultActionManager();
    }
    return manager;
  }
  
  /**
   * returns the slot the entity can equip the item. This checks if the entity 
   * has a slot for this item, not if the slot is used already.
   *
   * @return the slot name for the item or null if there is no matching slot
   *                 in the entity
   */
  public String canEquip(RPEntity entity, Item item)
  {
    // get all possible slots for this item
    List<String> slots = item.getPossibleSlots();

    for (String slot : slots)
    {
      if (entity.hasSlot(slot))
        {
        RPSlot rpslot=entity.getSlot(slot);
        if(!rpslot.isFull())   
          {
          return slot;
          }
        }
        
    }
    return null;
  }

  /** equipes the item in the specified slot */
  public boolean onEquip(RPEntity entity, String slotName, Item item)
  {
    if (!entity.hasSlot(slotName))
      return false;

// TODO: 
//    // recheck if the item can be equipped
//    if (!item.getPossibleSlots().contains(slotName))
//    {
//      logger.warn("tried to equip the item ["+item.getName()+"] in a nonsupported slot ["+slotName+"]");
//      return false;
//    }

    RPSlot rpslot = entity.getSlot(slotName);
    
    if (item instanceof Stackable)
    {
      Stackable stackEntity = (Stackable) item;
      // find a stackable item of the same type
      Iterator<RPObject> it = rpslot.iterator();
      while (it.hasNext())
      {
        RPObject object = it.next();
        if (object instanceof Stackable)
        {
          // found another stackable
          Stackable other = (Stackable) object;
          if (other.isStackable(stackEntity))
          {
            // other is the same type...merge them
            other.add(stackEntity);
            item  = null; // do not process the entity further
            break;
          }
        }
      }
    }
        
    // entity still there?
    if (item != null)
    {
      // yep, so it is stacked. simplay add it
      rpslot.assignValidID(item);
      rpslot.add(item);
    }

    return true;
  }

}
