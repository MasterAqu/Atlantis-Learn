package atlantis.combat.missions;

public abstract class MissionFocusPoint {

    /**
     * Position where units should concentrate around. Can be offensive or defensive. Usually around a choke.
     */
    public abstract AFocusPoint focusPoint();

}
