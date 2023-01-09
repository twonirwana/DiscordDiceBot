package de.janno.discord.bot.dice.image;

public class PolyhedralAlies extends PolyhedralFileImageProvider {

    @Override
    protected String getStyleFolder() {
        return "images/polyhedral_alies_blue_and_silver/";
    }

    @Override
    protected String getDieFolder(int totalDieSides) {
        return "d%d blue and silver/".formatted(totalDieSides);
    }

    @Override
    protected String getFileName(int totalDieSides, int shownDieSide) {
        return "blue and silver d%ds%d.png".formatted(totalDieSides, shownDieSide);
    }
}
