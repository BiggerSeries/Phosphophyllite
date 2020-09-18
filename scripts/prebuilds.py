import os
import sys

if __name__ == '__main__':
    if (os.path.exists("scripts")):
        os.chdir("scripts")
    # x = os.system("python3 process_shaders.py")
    # if (x != 0):
    #     sys.exit(2)
    x = os.system("python3 buildcpp.py")
    if (x != 0):
        sys.exit(2)
    pass
