function [hidden_weights] = bp_core(input_units,hidden_units,input_weights,in,hid,out,output_delta,target,output_units,hidden_delta,hidden_prev_weights,input_prev_weights,hidden_weights)
    % bpnn_train_kernel
    [input_units,hidden_units]  = bpnn_layerforward(input_units, hidden_units, input_weights, in, hid);
    [hidden_units,output_units] = bpnn_layerforward(hidden_units, output_units, hidden_weights, hid, out);
    [out_err,output_delta]      = bpnn_output_error(output_delta, target, output_units, out);
    [hid_err,hidden_delta]      = bpnn_hidden_error(hidden_delta,hid,output_delta,out,hidden_weights,hidden_units);
    [hidden_units,hidden_weights,hidden_prev_weights] = bpnn_adjust_weights(output_delta,out,hidden_units,hid,hidden_weights,hidden_prev_weights);
    [input_units,input_weights,input_prev_weights]    = bpnn_adjust_weights(hidden_delta,hid,input_units,in,input_weights,input_prev_weights);
end