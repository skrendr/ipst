/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.network.impl;

import eu.itesla_project.iidm.network.Terminal;
import eu.itesla_project.iidm.network.impl.util.Ref;
import gnu.trove.list.array.TIntArrayList;
import java.util.BitSet;
import java.util.List;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
abstract class TapChangerImpl<H extends TapChangerParent, C extends TapChangerImpl<H, C, S>, S extends TapChangerStepImpl<S>> implements Stateful {

    protected final Ref<? extends MultiStateObject> network;

    protected final H parent;

    protected int lowTapPosition;

    protected final List<S> steps;

    protected final TerminalExt terminal;

    // attributes depending on the state

    protected final TIntArrayList tapPosition;

    protected TapChangerImpl(Ref<? extends MultiStateObject> network, H parent,
                             int lowTapPosition, List<S> steps, TerminalExt terminal,
                             int tapPosition) {
        this.network = network;
        this.parent = parent;
        this.lowTapPosition = lowTapPosition;
        this.steps = steps;
        this.terminal = terminal;
        int stateArraySize = network.get().getStateManager().getStateArraySize();
        this.tapPosition = new TIntArrayList(stateArraySize);
        for (int i = 0; i < stateArraySize; i++) {
            this.tapPosition.add(tapPosition);
        }
    }

    protected abstract NetworkImpl getNetwork();

    public int getStepCount() {
        return steps.size();
    }

    public int getLowTapPosition() {
        return lowTapPosition;
    }

    public int getHighTapPosition() {
        return lowTapPosition + steps.size() - 1;
    }

    public int getTapPosition() {
        return tapPosition.get(network.get().getStateIndex());
    }

    protected abstract String getTapChangerAttribute();

    public C setTapPosition(int tapPosition) {
        if (tapPosition < lowTapPosition
                || tapPosition > getHighTapPosition()) {
            throw new ValidationException(parent, "incorrect tap position "
                    + tapPosition + " [" + lowTapPosition + ", "
                    + getHighTapPosition() + "]");
        }
        int oldValue = this.tapPosition.set(network.get().getStateIndex(), tapPosition);
        parent.getNetwork().getListeners().notifyUpdate(parent.getTransformer(), getTapChangerAttribute() + ".tapPosition", oldValue, tapPosition);
        return (C) this;
    }

    public S getStep(int tapPosition) {
        if (tapPosition < lowTapPosition || tapPosition > getHighTapPosition()) {
            throw new ValidationException(parent, "incorrect tap position "
                    + tapPosition + " [" + lowTapPosition + ", " + getHighTapPosition()
                    + "]");
        }
        return steps.get(tapPosition - lowTapPosition);
    }

    public S getCurrentStep() {
        return getStep(getTapPosition());
    }

    public TerminalExt getTerminal() {
        return terminal;
    }

    public void setTerminal(Terminal t) {
        if (terminal == null) {
            throw new ValidationException(parent, "regulation terminal is null");
        }
        if (terminal.getVoltageLevel().getNetwork() != getNetwork()) {
            throw new ValidationException(parent, "regulation terminal is not part of the network");
        }
    }

    @Override
    public void extendStateArraySize(int initStateArraySize, int number, int sourceIndex) {
        tapPosition.ensureCapacity(tapPosition.size() + number);
        for (int i = 0; i < number; i++) {
            tapPosition.add(tapPosition.get(sourceIndex));
        }
    }

    @Override
    public void reduceStateArraySize(int number) {
        tapPosition.remove(tapPosition.size() - number, number);
    }

    @Override
    public void deleteStateArrayElement(int index) {
        // nothing to do
    }

    @Override
    public void allocateStateArrayElement(int[] indexes, final int sourceIndex) {
        for (int index : indexes) {
            tapPosition.set(index, tapPosition.get(sourceIndex));
        }
    }

}
