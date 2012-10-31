package game.beans;

import java.io.Serializable;

import game.beans.pack.Pack;
import game.beans.pack.card.Card;
import game.beans.pack.card.rank.Rank;
import game.beans.pack.card.suit.Suit;
import game.logic.SantaseFacade;

public final class Game implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
     * Points of the game and other constants
     */
    public static final int END_GAME_POINTS = 66;

    public static final int POINTS_ZONE = 33;

    public static final int MAX_LITTLE_GAMES = 11;

    /**
     * Game cards.
     */
    private final Pack gameCards = new Pack();

    /**
     * Copy of game cards.
     */
    private final Pack copyGameCards = new Pack();

    /**
     * Computer player.
     */
    private final Player computer = new Player();

    /**
     * Human player.
     */
    private final Player human = new Player();

    /**
     * Trump suit.
     */
    private Suit trumpSuit;

    /**
     * Player which closed the game.
     */
    private Player playerClosedGame = null;

    /**
     * Copy of player which closed the game.
     */
    private Player copyPlayerClosedGame = null;

    /**
     * Player on move.
     */
    private Player trickAttackPlayer = human;

    /**
     * Copy of player on move.
     */
    private Player copyMove = getTrickAttackPlayer();

    /**
     * Game action constants.
     */
    public final static int GA_NONE = 0x00;

    public final static int GA_CLOSE = 0x01;

    public final static int GA_COUPLE = 0x02;

    public final static int GA_CHANGE = 0x04;

    private int gameActionStatus = GA_NONE;

    public final boolean containActionStatus(int status) {
        return (gameActionStatus & status) == status;
    }

    /**
     * Returns action status.
     * 
     * @return action status.
     */
    public final void clearGameActionStatus() {
        gameActionStatus = GA_NONE;
    }

    /**
     * Copies state.
     */
    public final void copyState() {
        human.copyState();
        computer.copyState();
        copyGameCards.fill(gameCards);
        copyPlayerClosedGame = playerClosedGame;
        copyMove = trickAttackPlayer;
    }

    /**
     * Restores state.
     */
    public final void restoreState() {
        human.restoreState();
        computer.restoreState();
        gameCards.fill(copyGameCards);
        playerClosedGame = copyPlayerClosedGame;
        setTrickAttackPlayer(copyMove);
    }

    /**
     * New game.
     */
    public final void newGame() {
        playerClosedGame = null;
        gameCards.fill(Pack.createFullPack());
        gameCards.shuffle();
        trumpSuit = gameCards.getCard(gameCards.getSize() - 1).getSuit();

        if (human.equals(trickAttackPlayer)) {
            human.newGame(gameCards);
            computer.newGame(gameCards);
        } else {
            computer.newGame(gameCards);
            human.newGame(gameCards);
        }
    }
    
    /**
     * New game.
     * @param player player.
     */
    public void newGame(final Player player) {
        calculatePoints(player);
        newGame();
    }

    /**
     * Checks move.
     */
    private void checkMove() {
        if (computer.getPlayedCard().getSuit().equals(human.getPlayedCard().getSuit())) {
            setTrickAttackPlayer((computer.getPlayedCard().getRank().compareTo(human.getPlayedCard().getRank()) > 0) ? computer : human);
        } else {
            if (computer.getPlayedCard().getSuit().equals(trumpSuit)) {
                setTrickAttackPlayer(computer);
            }
            if (human.getPlayedCard().getSuit().equals(trumpSuit)) {
                setTrickAttackPlayer(human);
            }
        }
    }

    /**
     * Adds hand.
     */
    private void addHand() {
        if (computer.equals(trickAttackPlayer)) {
            computer.getHands().add(computer.getPlayedCard());
            computer.getHands().add(human.getPlayedCard());
        } else {
            human.getHands().add(human.getPlayedCard());
            human.getHands().add(computer.getPlayedCard());
        }
    }

    /**
     * Fills one card.
     */
    private void fillOneCard() {
        if (!gameCards.isEmpty() && isNotClosedGame()) {
            if (human.equals(trickAttackPlayer)) {
                human.getCards().add(gameCards.remove(0));
                computer.getCards().add(gameCards.remove(0));
            }

            if (computer.equals(trickAttackPlayer)) {
                computer.getCards().add(gameCards.remove(0));
                human.getCards().add(gameCards.remove(0));
            }
        }
    }

    /**
     * Next tour.
     * @return boolean.
     */
    public final boolean nextTour() {
        checkMove();
        addHand();
        fillOneCard();

        computer.setPlayedCard(null);
        human.setPlayedCard(null);
        human.getCards().arrange();

        return !(computer.getCards().isEmpty() && human.getCards().isEmpty());
    }

    /**
     * Returns trump suit.
     * 
     * @return trump suit.
     */
    public final Suit getTrumpSuit() {
        return trumpSuit;
    }

    /**
     * Changes trump card.
     * 
     * @param card instance.
     * @param gPlayer player.
     */
    public final void changeTrumpCard(final Card card, final Player gPlayer) {
        if (!gameCards.isEmpty() && isNotClosedGame()) {
            Card trumpCard = gameCards.remove(gameCards.getSize() - 1);
            gPlayer.getCards().remove(card);
            gPlayer.getCards().add(trumpCard);
            gPlayer.getCards().arrange();
            gameCards.add(card);
        }
    }

    /**
     * Calculate game points.
     * 
     * @param playerOne player.
     * @param playerTwo player.
     */
    private void calculateGamePoints(Player playerOne, Player playerTwo) {
        if (isNotClosedGame()) {
            checkNormalPoints(playerOne, playerTwo);
            setTrickAttackPlayer(playerTwo);
            return;
        }

        if (playerOne.equals(playerClosedGame)) {
            if (playerOne.getPoints(trumpSuit) >= END_GAME_POINTS) {
                checkNormalPoints(playerOne, playerTwo);
                setTrickAttackPlayer(playerTwo);
            } else {
                playerTwo.setLittleGames(playerTwo.getLittleGames() + 3);
                setTrickAttackPlayer(playerOne);
            }
        }

        if (playerTwo.equals(playerClosedGame)) {
            playerOne.setLittleGames(playerOne.getLittleGames() + 3);
            setTrickAttackPlayer(playerTwo);
            return;
        }
    }

    public final void calculatePoints(final Player pMove) {
        if (pMove == computer) {
            calculateGamePoints(computer, human);
        }
        if (pMove == human) {
            calculateGamePoints(human, computer);
        }

        checkLittleGames();
    }

    private void checkNormalPoints(final Player winner, final Player loser) {
        if (loser.getPoints(trumpSuit) >= POINTS_ZONE) {
            winner.setLittleGames(winner.getLittleGames() + 1);
        } else if (loser.getHands().isEmpty()) {
            winner.setLittleGames(winner.getLittleGames() + 3);
        } else {
            winner.setLittleGames(winner.getLittleGames() + 2);
        }
    }

    private void checkLittleGames(final Player p1, final Player p2) {
        if (p1.getLittleGames() >= MAX_LITTLE_GAMES) {
            p1.setLittleGames(0);
            p2.setLittleGames(0);
            p1.setBigGames(p1.getBigGames() + 1);
        }
    }

    private void checkLittleGames() {
        checkLittleGames(computer, human);
        checkLittleGames(human, computer);
    }

    private boolean isNobodyPlayed() {
        return computer.getPlayedCard() == null && human.getPlayedCard() == null;
    }

    public final boolean isBothPlayed() {
        return computer.getPlayedCard() != null && human.getPlayedCard() != null;
    }

    private boolean isCloseGameZone() {
        return gameCards.getSize() != 12 && gameCards.getSize() > 2;
    }

    public final boolean canClose() {
        return isCloseGameZone() && isNobodyPlayed() && isNotClosedGame();
    }

    public final boolean canEndGame(final Player player) {
        if (player.equals(human)) {
            return computer.getPlayedCard() == null && human.getPoints(trumpSuit) >= SantaseFacade.END_GAME_POINTS;
        }
        if (player.equals(computer)) {
            return human.getPlayedCard() == null && computer.getPoints(trumpSuit) >= SantaseFacade.END_GAME_POINTS;
        }
        return false;
    }

    // changed 03.06.2005
    public final boolean isPlayerTurn(final Player player) {
        return player.equals(trickAttackPlayer);
    }

    public final boolean isPlayerClosed(final Player player) {
        return playerClosedGame == player;
    }

    public final boolean isClosedGame() {
        return playerClosedGame != null;
    }

    public final boolean isNotClosedGame() {
        return playerClosedGame == null;
    }

    public final void setClosedGame(final Player player) {
        gameActionStatus = gameActionStatus | GA_CLOSE;
        playerClosedGame = player;
    }

    public final boolean isObligatoryMode() {
        return gameCards.isEmpty() || isClosedGame();
    }

    public final void changeTrumpCard(final Player player) {
        if (!player.getHands().isEmpty() && getRival(player).getPlayedCard() == null && gameCards.getSize() > 2 && isNotClosedGame()) {
            Card card = player.getCards().findCard(Rank.Nine, getTrumpSuit());
            if (card != null) {
                gameActionStatus = gameActionStatus | GA_CHANGE;
                changeTrumpCard(card, player);
            }
        }
    }

    public final void setCoupleMessage(final Player player, final Card card) {
        player.getCouples().setCouple(card.getSuit());
        gameActionStatus = gameActionStatus | GA_COUPLE;
    }

    public final Player getTrickAttackPlayer() {
        return trickAttackPlayer;
    }

    public final void setTrickAttackPlayer(Player trickAttackPlayer) {
        this.trickAttackPlayer = trickAttackPlayer;
    }

    public final Player getPlayerClosedGame() {
        return playerClosedGame;
    }

    public final void setPlayerClosedGame(Player player) {
        this.playerClosedGame = player;
    }

    public final Player getRival(Player player) {
        if (player.equals(human)) {
            return computer;
        }

        return human;
    }

    public final Pack getGameCards() {
        return gameCards;
    }
    
    public final boolean isBigNewGame() {
        return (human.getBigGames() != 0 || computer.getBigGames() != 0) && (human.getLittleGames() == 0 && computer.getLittleGames() == 0);
    }

    public Player getComputer() {
        return computer;
    }

    public Player getHuman() {
        return human;
    }
}
