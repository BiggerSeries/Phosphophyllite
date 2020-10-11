package net.roguelogix.phosphophyllite.repack.jsonlogic.evaluator;

import net.roguelogix.phosphophyllite.repack.jsonlogic.JsonLogicException;

public class JsonLogicEvaluationException extends JsonLogicException {
  public JsonLogicEvaluationException(String msg) {
    super(msg);
  }

  public JsonLogicEvaluationException(Throwable cause) {
    super(cause);
  }

  public JsonLogicEvaluationException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
