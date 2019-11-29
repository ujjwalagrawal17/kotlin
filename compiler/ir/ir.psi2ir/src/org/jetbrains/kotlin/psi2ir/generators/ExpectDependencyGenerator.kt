package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.findExpects

// Need to create unbound symbols for expects corresponding to actuals of compiled module.
// This is neccessary because there is no explicit links between expects and actuals
// neither in descriptors nor in IR.
fun referenceExpectsForUsedActuals(symbolTable: SymbolTable, irModule: IrModuleFragment) {
    irModule.acceptVoid(object : IrElementVisitorVoid {

        private fun <T> T.forEachExpect(body: (DeclarationDescriptor) -> Unit) where T: IrDeclaration {
            this.descriptor.findExpects().forEach {
                body(it)
            }
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }
        override fun visitClass(declaration: IrClass) {
            declaration.forEachExpect { symbolTable.referenceClass(it as ClassDescriptor) }
            super.visitDeclaration(declaration)
        }
        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            declaration.forEachExpect { symbolTable.referenceSimpleFunction(it as FunctionDescriptor) }
            super.visitDeclaration(declaration)
        }
        override fun visitConstructor(declaration: IrConstructor) {
            declaration.forEachExpect { symbolTable.referenceConstructor(it as ClassConstructorDescriptor) }
            super.visitDeclaration(declaration)
        }
        override fun visitProperty(declaration: IrProperty) {
            declaration.forEachExpect { symbolTable.referenceProperty(it as PropertyDescriptor) }
            super.visitDeclaration(declaration)
        }
        override fun visitTypeAlias(declaration: IrTypeAlias) {
            declaration.forEachExpect {
                when (it) {
                    is ClassDescriptor -> symbolTable.referenceClass(it)
                    else -> error("Unexpected expect for actual type alias: $it")
                }
            }
            super.visitDeclaration(declaration)
        }
    })
}
