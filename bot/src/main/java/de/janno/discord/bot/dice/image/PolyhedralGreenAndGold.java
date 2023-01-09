package de.janno.discord.bot.dice.image;

public class PolyhedralGreenAndGold extends PolyhedralFileImageProvider {

    @Override
    protected String getStyleFolder() {
        return "images/polyhedral_green_and_gold/";
    }

    @Override
    protected String getDieFolder(int totalDieSides) {
        return "d%d green and gold/".formatted(totalDieSides);
    }

    @Override
    protected String getFileName(int totalDieSides, int shownDieSide) {
        return "green and gold d%ds%d.png".formatted(totalDieSides, shownDieSide);
    }
}
