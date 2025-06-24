package sclient.mazenet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.fhac.mazenet.server.game.Board;
import de.fhac.mazenet.server.game.Card;
import de.fhac.mazenet.server.game.Position;
import de.fhac.mazenet.server.generated.AwaitMoveMessageData;
import de.fhac.mazenet.server.generated.BoardData;
import de.fhac.mazenet.server.generated.CardData;
import de.fhac.mazenet.server.generated.MoveMessageData;
import de.fhac.mazenet.server.generated.PositionData;
import de.fhac.mazenet.server.generated.Treasure;

public class Strategy {
    private int playerId;

    public Strategy(int id) {
        this.playerId = id;
    }

    public MoveMessageData startAlgo(AwaitMoveMessageData awaitMove) {
        BoardData boardData = awaitMove.getBoard();
        Board prevBoard = new Board(boardData);
        Treasure treasure = awaitMove.getTreasureToFindNext();

        List<MoveMessageData> possibleMoves = generatePossibleMoves(prevBoard);

        MoveMessageData winningMove = findWinningMove(possibleMoves, prevBoard, treasure);
        if (winningMove != null) {
            return winningMove;
        }

        MoveMessageData potentialWinningMove = findPotentialWinningMove(possibleMoves, prevBoard, treasure);
        if (potentialWinningMove != null) {
            return potentialWinningMove;
        }

        return getRandomMove(possibleMoves);
    }

    private List<MoveMessageData> generatePossibleMoves(Board prevBoard) {
        List<Position> shiftMoves = Position.getPossiblePositionsForShiftcard();
        shiftMoves.remove(prevBoard.getForbidden());

        List<MoveMessageData> possibleMoves = new ArrayList<>();
        for (Position position : shiftMoves) {
            Card card = new Card(prevBoard.getShiftCard());
            List<Card> orientedShiftCards = card.getPossibleRotations();
            for (Card orientedShiftCard : orientedShiftCards) {
                MoveMessageData move = new MoveMessageData();
                move.setShiftPosition(position);
                move.setShiftCard(orientedShiftCard);
                move.setNewPinPos(prevBoard.fakeShift(move).findPlayer(this.playerId));
                possibleMoves.add(move);
            }
        }

        return possibleMoves;
    }

    private MoveMessageData findWinningMove(List<MoveMessageData> possibleMoves, Board prevBoard, Treasure treasure) {
        for (MoveMessageData move : possibleMoves) {
            Board nextBoard = prevBoard.fakeShift(move);
            PositionData treasurePositionData = nextBoard.findTreasure(treasure);
            if (treasurePositionData == null) continue;

            Position treasurePosition = new Position(treasurePositionData);
            List<Position> reachablePositions = nextBoard.getAllReachablePositions(nextBoard.findPlayer(this.playerId));

            if (reachablePositions.contains(treasurePosition)) {
                move.setNewPinPos(treasurePosition);
                return move;
            }
        }

        return null;
    }

    private MoveMessageData findPotentialWinningMove(List<MoveMessageData> possibleMoves, Board prevBoard, Treasure treasure) {
        List<Position> potentialShiftMoves = Position.getPossiblePositionsForShiftcard();
        potentialShiftMoves.remove(prevBoard.getForbidden());

        for (MoveMessageData myMove : possibleMoves) {
            Board board = prevBoard.fakeShift(myMove);
            for (Position position : potentialShiftMoves) {
                Card card = new Card(board.getShiftCard());
                List<Card> orientedShiftCards = card.getPossibleRotations();
                for (Card orientedShiftCard : orientedShiftCards) {
                    MoveMessageData potentialMove = new MoveMessageData();
                    potentialMove.setShiftPosition(position);
                    potentialMove.setShiftCard(orientedShiftCard);
                    potentialMove.setNewPinPos(board.fakeShift(potentialMove).findPlayer(this.playerId));

                    List<Position> reachablePositions = board.getAllReachablePositions(board.findPlayer(this.playerId));
                    PositionData treasurePositionData = board.findTreasure(treasure);
                    if (treasurePositionData == null) continue;

                    Position treasurePosition = new Position(treasurePositionData);
                    if (reachablePositions.contains(treasurePosition)) {
                        return myMove;
                    }
                }
            }
        }

        return null;
    }

    private MoveMessageData getRandomMove(List<MoveMessageData> possibleMoves) {
        return possibleMoves.get(new Random().nextInt(possibleMoves.size()));
    }
}
