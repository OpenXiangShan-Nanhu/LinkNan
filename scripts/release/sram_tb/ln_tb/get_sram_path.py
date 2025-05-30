import re

sim_result = ""
with open('run_log_ln.txt', 'r') as file:
    content = file.read()
    if 'TEST CASE PASS' in content:
        print("TEST CASE PASS")
        sim_result = "TEST CASE PASS"
    if 'TEST CASE FAIL' in content:
        print("TEST CASE FAIL")
        sim_result = "TEST CASE FAIL"


def extract_and_append(input_file_path, output_file_path):
    with open(input_file_path, 'r') as file:
        file_content = file.read()
    pattern = re.compile(r'dut_inst\.(.*?)}', re.DOTALL)
    matches = pattern.findall(file_content)
    with open(output_file_path, 'a') as file:
        file.write(f"for this case:\n")
        for match in matches:
            for line in match.splitlines():
                file.write(line + '\n')
        file.write(f"total {len(matches)} sram has been verified\n")
        file.write(f"{sim_result}\n")
extract_and_append('search.log', 'run_log_ln.txt')


