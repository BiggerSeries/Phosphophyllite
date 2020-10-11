package net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator.expressions;

import net.roguelogix.phosphophyllite.repack.jsonlogic.utils.ArrayLike;
import net.roguelogix.phosphophyllite.repack.jsonlogic.ast.JsonLogicArray;
import net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator.JsonLogicEvaluationException;
import net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator.JsonLogicEvaluator;
import net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator.JsonLogicExpression;

import java.util.List;

public interface PreEvaluatedArgumentsExpression extends JsonLogicExpression {
  Object evaluate(List arguments, Object data) throws JsonLogicEvaluationException;

  @Override
  default Object evaluate(JsonLogicEvaluator evaluator, JsonLogicArray arguments, Object data)
    throws JsonLogicEvaluationException {
    List<Object> values = evaluator.evaluate(arguments, data);

    if (values.size() == 1 && ArrayLike.isEligible(values.get(0))) {
      values = new ArrayLike(values.get(0));
    }

    return evaluate(values, data);
  }
}
