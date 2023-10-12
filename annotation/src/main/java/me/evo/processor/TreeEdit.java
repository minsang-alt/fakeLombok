package me.evo.processor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.util.Objects;
import java.util.function.Consumer;


public class TreeEdit {
    private Trees trees;
    private Context context;
    private TreeMaker treeMaker;
    private Names names;
    private TreePathScanner<Object, CompilationUnitTree> scanner;

    /**
     * Javac 에서 사용하는 객체를 생성하여 멤버변수들을 초기화
     * 구문트리를 조작하기 위해 사용
     * Create an object used by Javac to initialize  AST member variables
     */
    TreeEdit(ProcessingEnvironment processingEnvironment){

        final JavacProcessingEnvironment javacProcessingEnvironment = (JavacProcessingEnvironment) processingEnvironment;

        // (AST, 추상 구문 트리)에 접근할 수 있는 유틸리티
        this.trees = Trees.instance(processingEnvironment);
        // 컴파일 과정에서 필요한 다양한 객체들을 저장하는 데 사용
        this.context = javacProcessingEnvironment.getContext();
        // AST를 생성하는데 도움을 줍니다.
        this.treeMaker = TreeMaker.instance(context);
        //식별자
        this.names = Names.instance(context);
    }

    /**
     * 'TreePathScanner' 클래스는 AST(추상 구문 트리) 위에서 순회하는 기능을 제공합니다.
     * CompilationUnitTree 타입 파라미터가 의미하는 것은 스캐너가 'CompilationUnit' (즉, 전체 Java 소스 파일 하나) 단위로 작동한다는 것 입니다.
     * 스캔 동안 각 노드에서 원하는 작업을 수행하기 위해 'TreePathScanner' 클래스를 확장하여 사용자 정의 방문자(visitor) 메서드를 제공하게 됩니다
     * @param strategy
     */

    public void setClassDefModifyStrategy(Consumer<JCTree.JCClassDecl> strategy){
        this.scanner = new TreePathScanner<Object, CompilationUnitTree>(){
            @Override
            public Trees visitClass(ClassTree classTree,CompilationUnitTree unitTree){
                JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) unitTree;
                //소스파일인 경우만 새로운 'TreeTranslator' 객체를 생성하고 그것에게 해당 노드를 방문하도록 요청
                if(compilationUnit.sourcefile.getKind() == JavaFileObject.Kind.SOURCE){
                    compilationUnit.accept(new TreeTranslator(){
                        @Override
                        public void visitClassDef(JCTree.JCClassDecl jcClassDecl){
                            super.visitClassDef(jcClassDecl);

                            //실제 AST 수정이 이뤄지는 부분(구체적인 전략은 parameter로 전달받음)
                            strategy.accept(jcClassDecl);
                        }
                    });
                }
                return trees;
            }
        };
    }

    //AST를 순회하며 AST를 조작
    public void modifyTree(Element element) {
        if (Objects.nonNull(scanner)) {
            final TreePath path = trees.getPath(element);
            scanner.scan(path, path.getCompilationUnit());
        }
    }

    public TreeMaker getTreeMaker() {
        return this.treeMaker;
    }

    public Names getNames() {
        return this.names;
    }

}
