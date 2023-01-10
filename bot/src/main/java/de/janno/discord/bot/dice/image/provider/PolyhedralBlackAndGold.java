package de.janno.discord.bot.dice.image.provider;

public class PolyhedralBlackAndGold extends PolyhedralFileImageProvider {

    @Override
    protected String getStyleFolder() {
        return "images/polyhedral_black_and_gold/";
    }

    @Override
    protected String getDieFolder(int totalDieSides) {
        return "d%d/".formatted(totalDieSides);
    }

    @Override
    protected String getFileName(int totalDieSides, int shownDieSide) {
        return "d%ds%d.png".formatted(totalDieSides, shownDieSide);
    }
}
