package de.janno.discord.bot.dice.image;

public class PolyhedralRedAndGold extends PolyhedralFileImageProvider {

    @Override
    protected String getStyleFolder() {
        return "images/polyhedral_red_and_gold/";
    }

    @Override
    protected String getDieFolder(int totalDieSides) {
        return "d%d red and gold/".formatted(totalDieSides);
    }

    @Override
    protected String getFileName(int totalDieSides, int shownDieSide) {
        return "red and gold d%ds%d.png".formatted(totalDieSides, shownDieSide);
    }
}
