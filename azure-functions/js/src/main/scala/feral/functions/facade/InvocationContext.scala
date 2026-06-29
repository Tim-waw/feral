package feral.functions.facade

import scala.scalajs.js

@js.native
trait InvocationContext extends js.Object {
  def log(args: js.Any*): Unit = js.native
  def trace(args: js.Any*): Unit = js.native
  def debug(args: js.Any*): Unit = js.native
  def info(args: js.Any*): Unit = js.native
  def warn(args: js.Any*): Unit = js.native
  def error(args: js.Any*): Unit = js.native
}

/* 
invocationId: string;
    functionName: string;
    extraInputs: InvocationContextExtraInputs;
    extraOutputs: InvocationContextExtraOutputs;
    log(...args: any[]): void;
    trace(...args: any[]): void;
    debug(...args: any[]): void;
    info(...args: any[]): void;
    warn(...args: any[]): void;
    error(...args: any[]): void;
    retryContext?: RetryContext;
    traceContext?: TraceContext;
    triggerMetadata?: TriggerMetadata;
    options: EffectiveFunctionOptions;
 */