package net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator.expressions;

import net.roguelogix.phosphophyllite.repack.jsonlogic.utils.ArrayLike;
import net.roguelogix.phosphophyllite.repack.jsonlogic.JsonLogic;
import net.roguelogix.phosphophyllite.repack.jsonlogic.ast.JsonLogicArray;
import net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator.JsonLogicEvaluationException;
import net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator.JsonLogicEvaluator;
import net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator.JsonLogicExpression;

import java.util.ArrayList;
import java.util.List;

public class FilterExpression implements JsonLogicExpression {
  public static final FilterExpression INSTANCE = new FilterExpression();

  private FilterExpression() {
    // Use INSTANCE instead.
  }

  @Override
  public String key() {
    return "filter";
  }

  @Override
  public Object evaluate(JsonLogicEvaluator evaluator, JsonLogicArray arguments, Object data)
    throws JsonLogicEvaluationException {
    if (arguments.size() != 2) {
      throw new JsonLogicEvaluationException("filter expects exactly 2 arguments");
    }

    Object maybeArray = evaluator.evaluate(arguments.get(0), data);

    if (!ArrayLike.isEligible(maybeArray)) {
      throw new JsonLogicEvaluationException("first argument to filter must be a valid array");
    }

    List<Object> result = new ArrayList<>();

    for (Object item : new ArrayLike(maybeArray)) {
      if(JsonLogic.truthy(evaluator.evaluate(arguments.get(1), item))) {
        result.add(item);
      }
    }

    return result;
  }
}
