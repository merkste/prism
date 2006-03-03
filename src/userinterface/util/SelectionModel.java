//==============================================================================
//	
//	Copyright (c) 2002-2004, Andrew Hinton, Dave Parker
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package userinterface.util;
import java.util.ArrayList;
import java.util.*;
import javax.swing.event.EventListenerList;

/**
 * Handles the selection of PropertyOwners
 * @author  ug60axh
 */
public abstract class SelectionModel extends Observable
{
    
    private ArrayList listeners;
    private ArrayList currentSelection; 
    
    public SelectionModel()
    {
        listeners = new ArrayList();
        currentSelection = new ArrayList();
    }
    
    public void addSelectionListener(SelectionListener s)
    {
        listeners.add(s);
    }
    
    public void removeSelectionListener(SelectionListener s)
    {
        listeners.remove(s);
    }
    
    public void fireSelectionChanged()
    {
        ////System.out.println("firing selection changed");
        SelectionEvent event = new SelectionEvent(getCurrentSelection());
        SelectionListener sl;
        
        
        for(int i = 0; i < listeners.size(); i++)
        {
            
            sl = (SelectionListener)listeners.get(i);
            sl.selectionPerformed(event);
        }
    }
    
        public ArrayList getCurrentSelection()
        {
            return currentSelection;
        }        
    
        public PropertyOwner getSelectedItem(int i)
        {
            return (PropertyOwner)currentSelection.get(i);
        }        
    
        public int getSelectionSize()
        {
            return currentSelection.size();
        }
        
        protected void addToSelection(PropertyOwner owner, boolean fireEvent)
        {
            if(!currentSelection.contains(owner))
                currentSelection.add(owner);
            
            if(fireEvent)fireSelectionChanged();
        }
        
        protected void removeFromSelection(PropertyOwner owner, boolean fireEvent)
        {
            if(currentSelection.contains(owner))
            currentSelection.remove(owner);
            
            if(fireEvent)fireSelectionChanged();
        }
        
        protected void clearSelection(boolean fireEvent)
        {
            currentSelection.clear();
            
            if(fireEvent)fireSelectionChanged();
        }
        
        protected void setSelection(ArrayList a, boolean fireEvent)
        {
            currentSelection = a;
            
            if(fireEvent)fireSelectionChanged();
        }
}
