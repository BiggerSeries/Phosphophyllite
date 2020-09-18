import os.path
import sys

if __name__ == '__main__':
    os.chdir("../src/main/quartzpp")
    if (os.path.exists("cmake-build-debug")):
        os.chdir("cmake-build-debug")
    else:
        os.system("rm build")
        os.system("mkdir build")
        os.chdir("build")
        os.system("cmake ")
        pass
    x = os.system("make quartzpp -j ${nproc}")
    if(x != 0):
        sys.exit(2)
    os.system("mkdir -p ../../resources/assets/phosphophyllite/libs/")
    os.system("cp libquartzpp.so ../../resources/assets/phosphophyllite/libs/")
    os.system("cp ./RogueLib/RogueLib/Exceptions/libRogueLib_Exceptions.so ../../resources/assets/phosphophyllite/libs/libroguelib_exceptions.so")
    os.system("cp ./RogueLib/RogueLib/Threading/libRogueLib_Threading.so ../../resources/assets/phosphophyllite/libs/libroguelib_threading.so")
    os.system("cp ./RogueLib/RogueLib/Logging/libRogueLib_Logging.so ../../resources/assets/phosphophyllite/libs/libroguelib_logging.so")
    os.chdir("../../../../")
pass
