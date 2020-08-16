/*
 * BetonQuest - advanced quests for Bukkit
 * Copyright (C) 2016  Jakub "Co0sh" Sapalski
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
package pl.betoncraft.betonquest.id;

import pl.betoncraft.betonquest.Instruction;
import pl.betoncraft.betonquest.config.Config;
import pl.betoncraft.betonquest.config.ConfigPackage;
import pl.betoncraft.betonquest.exceptions.ObjectNotFoundException;

@SuppressWarnings("PMD.ShortClassName")
public abstract class ID {

    public static final String UP_STR = "_"; // string used as "up the hierarchy" package

    protected String id;
    protected ConfigPackage pack;
    protected Instruction instruction;
    protected String rawInstruction;

    public ID(final ConfigPackage pack, final String id) throws ObjectNotFoundException {

        // id must be specified
        if (id == null || id.length() == 0) {
            throw new ObjectNotFoundException("ID is null");
        }

        // resolve package name
        if (id.contains(".")) {
            // id has specified a package, get it!
            final int dotIndex = id.indexOf('.');
            final String packName = id.substring(0, dotIndex);
            if (pack != null && packName.startsWith(UP_STR + "-")) {
                // resolve relative name if we have a supplied package
                final String[] root = pack.getName().split("-");
                final String[] path = packName.split("-");
                // count how many packages up we need to go
                int stepsUp = 0;
                while (stepsUp < path.length && path[stepsUp].equals(UP_STR)) {
                    stepsUp++;
                }
                // can't go out of BetonQuest folder of course
                if (stepsUp > root.length) {
                    throw new ObjectNotFoundException("Relative path goes out of package scope! Consider removing a few '"
                            + UP_STR + "'s in ID " + id);
                }
                // construct the final absolute path
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < root.length - stepsUp; i++) {
                    sb.append(root[i] + '-');
                }
                for (int i = stepsUp; i < path.length; i++) {
                    sb.append(path[i] + '-');
                }
                final String absolute = sb.substring(0, sb.length() - 1);
                this.pack = Config.getPackages().get(absolute);
                // throw error earlier so it can have more information than default one at the bottom
                if (this.pack == null) {
                    throw new ObjectNotFoundException("Relative path in ID '" + id + "' resolved to '" + absolute +
                            "', but this package does not exist");
                }
            } else {
                // use package name as absolute path if no relative path is available
                this.pack = Config.getPackages().get(packName);
            }
            if (id.length() == dotIndex + 1) {
                throw new ObjectNotFoundException("ID of the pack '" + this.pack + "' is null");
            }
            this.id = id.substring(dotIndex + 1);
        } else {
            // id does not specify package, use supplied package
            if (pack == null) {
                this.pack = Config.getDefaultPackage();
            } else {
                this.pack = pack;
            }
            this.id = id;
        }

        // no package yet? this is an error
        if (this.pack == null) {
            throw new ObjectNotFoundException("Package in ID '" + id + "' does not exist");
        }
    }

    public ConfigPackage getPackage() {
        return pack;
    }

    public String getBaseID() {
        return id;
    }

    public String getFullID() {
        return pack.getName() + "." + getBaseID();
    }

    @Override
    public String toString() {
        return getFullID();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof ID) {
            final ID id = (ID) o;
            return id.id.equals(this.id) &&
                    id.pack.equals(this.pack);
        }
        return false;
    }

    public Instruction generateInstruction() {
        if (rawInstruction == null) {
            return null;
        }
        if (instruction == null) {
            instruction = new Instruction(pack, this, rawInstruction);
        }
        return instruction;
    }

}