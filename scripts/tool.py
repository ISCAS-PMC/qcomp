from decimal import Decimal
from benchmark import Benchmark
from invocation import Invocation
from execution import Execution


def get_name():
    """ should return the name of the tool as listed on http://qcomp.org/competition/2019/"""
    return "ePMC"

def is_benchmark_supported(benchmark : Benchmark):
    """ Auxiliary function that returns True if the provided benchmark is supported by the tool"""

    if benchmark.is_pta():
        # PTAs are not supported by ePMC
        return False
    if benchmark.is_prism_ma():
        # MAs are not supported by ePMC
        return False
    if benchmark.get_short_property_type() == "S":
		# Steady state properties are not supported by ePMC
        return False
    if benchmark.is_prism_inf():
		# CTMCs with infinite state-spaces are not supported by ePMC
        return False
    
    return True

def is_on_benchmark_list(benchmark : Benchmark):
    """ Returns true, if the given benchmark should appear on the generated benchmark list."""

    # do not include for models that ePMC does not support
    if not is_benchmark_supported(benchmark):
        return False

    # do not include models with small state spaces, except it's the haddad-monmege one (because we like that)
    # if benchmark.get_max_num_states() is not None and benchmark.get_max_num_states() < 10000 and benchmark.get_model_short_name() != "haddad-monmege":
    #    return False
    # list of input models ePMC does not support
    if benchmark.is_galileo():
        return False
    if benchmark.is_greatspn():
        return False
    if benchmark.is_modest():
        return False
    if benchmark.is_ppddl():
        return False
    if benchmark.is_pgcl():
        return False
    if benchmark.is_modest():
        return False
    return True

def get_invocations(benchmark : Benchmark):
    """
    Returns a list of invocations that invoke the tool for the given benchmark.
    It can be assumed that the current directory is the directory from which execute_invocations.py is executed.
    For QCOMP 2019, this should return a list of size at most two, where
    the first entry (if present) corresponds to the default configuration of the tool and
    the second entry (if present) corresponds to an optimized setting (e.g., the fastest engine and/or solution technique for this benchmark).
    Please only provide two invocations if there is actually a difference between them.
    If this benchmark is not supported, an empty list has to be returned.
    For testing purposes, the script also allows to return more than two invocations.
    """

    if not is_benchmark_supported(benchmark):
        return []

    # Gather options that are needed for this particular benchmark for any invocation of ePMC
    preprocessing_steps = []
    preprocessing_notes = []
    benchmark_settings = ""
    if benchmark.is_prism():
		# set parameters
        benchmark_settings = "--model-input-files {} --property-input-files {} --property-input-names {} --translate-messages false".format(benchmark.get_prism_program_filename(), benchmark.get_prism_property_filename(), benchmark.get_property_name())
        if benchmark.get_open_parameter_def_string() != "":
            benchmark_settings += " --const {}".format(benchmark.get_open_parameter_def_string())
        # if benchmark.is_ctmc():
        #    benchmark_settings += " --prismcompat"
    else:
        # put properties in saparate files 
        benchmark_settings = "--model-input-files {} --model-input-type jani --property-input-names {} --translate-messages false".format(benchmark.get_janifilename(), benchmark.get_property_name())
        if benchmark.get_open_parameter_def_string() != "":
            benchmark_settings += " --const {}".format(benchmark.get_open_parameter_def_string())

    invocations = []


    # default settings
    default_inv = Invocation()
    default_inv.identifier = "default"
    default_inv.note = "Default settings."
    if len(preprocessing_steps) != 0:
        for prep in preprocessing_steps:
            default_inv.add_command(prep)
        default_inv.note += " " + " ".join(preprocessing_notes)
    memsize = "12288m"
    default_inv.add_command("java -Xms{} -Xmx{} -jar ./epmc-standard.jar check {}".format(memsize, memsize, benchmark_settings))
    invocations.append(default_inv)

    # specific settings
    # dd engine 
    if benchmark.get_short_property_type() == "E" or benchmark.get_short_property_type() == "Ei" or benchmark.get_short_property_type() == "Eb":
        return invocations
    specific_inv = Invocation()
    specific_inv.identifier = "specific"
    specific_inv.note = "Settings specific for this benchmark. Use symbolic model checking algorithms with BDDs"
    specific_inv.note += " " + " ".join(preprocessing_notes)
    specific_inv.add_command("java -Xms{} -Xmx{} -jar ./epmc-standard.jar check {} --engine dd".format(memsize, memsize, benchmark_settings))
    invocations.append(specific_inv)

    return invocations


def get_result(benchmark : Benchmark, execution : Execution):
    """
    Returns the result of the given execution on the given benchmark.
    This method is called after executing the commands of the associated invocation.
    One can either find the result in the tooloutput (as done here) or
    read the result from a file that the tool has produced.
    The returned value should be either 'true', 'false', or a decimal number.
    """
    invocation = execution.invocation
    log = execution.concatenate_logs()
    pos = log.find("{}:".format(benchmark.get_property_name()))
    if pos < 0:
        return None
    pos = pos + len(benchmark.get_property_name() + ":")
    eol_pos = log.find("\n", pos)
    return log[pos:eol_pos].strip()
