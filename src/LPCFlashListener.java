/*
 * Part of CANFlasherUTNL - Flash tool for NXP LPC11C22/24 devices.
 * http://www.fischl.de/can/bootloader/canflasherutnl/
 *
 * Copyright (C) 2016  Thomas Fischl 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Interface to listen to output messages of LPC flashing
 * 
 * @author Thomas Fischl
 */
public interface LPCFlashListener {
    
    /**
     * Print out given message
     * 
     * @param message Message to print out
     */
    public void outputMessage(String message);
}