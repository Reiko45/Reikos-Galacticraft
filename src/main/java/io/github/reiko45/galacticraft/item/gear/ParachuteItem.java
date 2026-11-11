package io.github.reiko45.galacticraft.item.gear;

/**
 * Parachute (1.12.2 ItemParaChute, 16 colours as metadata). Worn in the parachute slot; it opens when the player
 * comes down from orbit, which arrives with rockets and landers.
 */
public class ParachuteItem extends GearItem {
    /** 1.12.2 colour names, in metadata order; also the parachute model texture names. */
    public static final String[] LEGACY_COLORS = {"plain", "black", "blue", "lime", "brown", "darkblue", "darkgray", "darkgreen",
            "gray", "magenta", "orange", "pink", "purple", "red", "teal", "yellow"};
    /** 1.21 item ids for those colours ("blue" was light blue, "darkblue" blue, "gray" light gray, "teal" cyan...). */
    public static final String[] IDS = {"parachute", "black_parachute", "light_blue_parachute", "lime_parachute",
            "brown_parachute", "blue_parachute", "gray_parachute", "green_parachute", "light_gray_parachute",
            "magenta_parachute", "orange_parachute", "pink_parachute", "purple_parachute", "red_parachute", "cyan_parachute",
            "yellow_parachute"};

    private final int color;

    public ParachuteItem(Properties properties, int color) {
        super(properties, GearSlot.PARACHUTE);
        this.color = color;
    }

    /** 1.12.2 metadata colour index. */
    public int color() {
        return this.color;
    }
}
