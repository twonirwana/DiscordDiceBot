package de.janno.discord.bot.dice.image;

public class PolyhedralOrangeAndSilver extends PolyhedralFileImageProvider {

    @Override
    protected String getStyleFolder() {
        return "images/polyhedral_orange_and_silver/";
    }

    @Override
    protected String getDieFolder(int totalDieSides) {
        return "d%d orange and silver/".formatted(totalDieSides);
    }

    @Override
    protected String getFileName(int totalDieSides, int shownDieSide) {
        return "orange and silver d%ds%d.png".formatted(totalDieSides, shownDieSide);
    }
}
