package net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator;

import net.roguelogix.phosphophyllite.repack.jsonlogic.ast.JsonLogicArray;

public interface JsonLogicExpression {
  String key();

  Object evaluate(JsonLogicEvaluator evaluator, JsonLogicArray arguments, Object data)
    throws JsonLogicEvaluationException;
}
